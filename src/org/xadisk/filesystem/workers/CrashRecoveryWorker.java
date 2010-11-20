package org.xadisk.filesystem.workers;

import org.xadisk.filesystem.utilities.FileIOUtility;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkManager;
import org.xadisk.connector.inbound.EndPointActivation;
import org.xadisk.filesystem.FileStateChangeEvent;
import org.xadisk.filesystem.NativeSession;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.TransactionLogEntry;
import org.xadisk.filesystem.XidImpl;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;
import org.xadisk.filesystem.utilities.TransactionLogsUtility;

public class CrashRecoveryWorker implements Work {

    private final NativeXAFileSystem xaFileSystem;
    private final HashMap<XidImpl, ArrayList<Long>> transactionLogPositions = new HashMap<XidImpl, ArrayList<Long>>(1000);
    private final HashMap<Integer, FileChannel> logChannels = new HashMap<Integer, FileChannel>(5);
    private final HashSet<XidImpl> preparedInDoubtTransactions = new HashSet<XidImpl>(1000);
    private final HashSet<XidImpl> onePhaseCommittingTransactions = new HashSet<XidImpl>(1000);
    private final HashSet<XidImpl> heavyWriteTransactionsForRollback = new HashSet<XidImpl>(1000);
    private volatile boolean released = false;
    private volatile boolean logFilesCleaned = false;
    private final HashMap<XidImpl, HashSet> transactionsAndFilesWithLatestViewOnDisk = new HashMap<XidImpl, HashSet>(1000);
    private final ArrayList<XidImpl> committedTransactions = new ArrayList<XidImpl>(1000);
    private final HashMap<XidImpl, ArrayList<FileStateChangeEvent>> eventsEnqueuePreparedOnly =
            new HashMap<XidImpl, ArrayList<FileStateChangeEvent>>(1000);
    private final ArrayList<FileStateChangeEvent> eventsEnqueueCommittedNotDequeued =
            new ArrayList<FileStateChangeEvent>(1000);
    private final ArrayList<FileStateChangeEvent> eventsDequeueCommitted = new ArrayList<FileStateChangeEvent>(1000);
    private final HashMap<XidImpl, FileStateChangeEvent> eventsDequeuePrepared = new HashMap<XidImpl, FileStateChangeEvent>(1000);
    private final HashMap<XidImpl, Integer> transactionsLatestCheckPoint = new HashMap<XidImpl, Integer>(1000);
    private final ArrayList<EndPointActivation> remoteActivations = new ArrayList<EndPointActivation>();
    private final AtomicInteger distanceFromRecoveryCompletion = new AtomicInteger(0);

    public CrashRecoveryWorker(NativeXAFileSystem xaFileSystem) {
        this.xaFileSystem = xaFileSystem;
    }

    private void collectLogFileNamesToProcess() throws IOException {
        String logsDir = xaFileSystem.getTransactionLogsDir();
        String logNames[] = FileIOUtility.listDirectoryContents(new File(logsDir));

        for (int i = 0; i < logNames.length; i++) {
            int logIndex = Integer.parseInt(logNames[i].split("_")[1]);
            logChannels.put(logIndex, new FileInputStream(logsDir + File.separator + logNames[i]).getChannel());
        }
    }

    public void release() {
        released = true;
    }

    private void cleanLogFiles() throws IOException {
        Iterator iter = logChannels.keySet().iterator();
        while (iter.hasNext()) {
            int logIndex = (Integer) iter.next();
            FileChannel logFC = (FileChannel) logChannels.get(logIndex);
            logFC.close();
            FileIOUtility.deleteFile(new File(xaFileSystem.getTransactionLogFileBaseName() + "_" + logIndex));
        }
    }

    public void collectRecoveryData() throws IOException {
        //earlier we had all the "content" of this method inside the run method, and the run
        //method used to run "from back of the bootup", i.e. the bootup process only started this
        //recovery worker, and everything would happen asynchly and most likely after completing the
        //bootup. The problem there was a probable call from TM "recover()" where XADisk was supposed
        //to return all prepared transactions; what if recovery worker hasn't completed collecting
        //these prepared txn list. So, we came to the current solution: collect data inside booting.
        //Changes to THIS object will be made in this method using the booting thread and later used
        //by the worker thread; but that is not a problem as the worker thread would be created after
        //completion of this method; so changes would be reflected across probable different "processors".
        collectLogFileNamesToProcess();
        for (FileChannel logCh : logChannels.values()) {
            if (released) {
                return;
            }
            findInCompleteTransactions(logCh.position(0));
        }
        for (Integer logIndex : logChannels.keySet()) {
            if (released) {
                return;
            }
            collectTransactionLogPositions(logChannels.get(logIndex).position(0), logIndex);
        }
    }

    public void run() {
        try {
            registerRemoteEndpoints();
            recoverOnePhaseTransactions();
            recoverHeavyWriteTransactionsForRollback();
            prepareEventsToPopulate();
            distanceFromRecoveryCompletion.set(preparedInDoubtTransactions.size()
                    + onePhaseCommittingTransactions.size() + eventsDequeuePrepared.size()
                    + heavyWriteTransactionsForRollback.size());
            checkForRecoveryDone();
        } catch (Throwable t) {
            xaFileSystem.notifySystemFailure(t);
        }
    }

    private void checkForRecoveryDone() throws IOException {
        boolean recoveryDone = distanceFromRecoveryCompletion.get() == 0;
        if (recoveryDone && !logFilesCleaned && distanceFromRecoveryCompletion.compareAndSet(0, -11)) {
            //notify before cleaning the log files so that epActivation first happen and get recorded
            //in the newer logs.
            xaFileSystem.notifyRecoveryComplete();
            cleanLogFiles();
            logFilesCleaned = true;
        }
    }

    private void findInCompleteTransactions(FileChannel logFC) throws IOException {
        TransactionLogEntry logEntry = null;
        while (true) {
            try {
                logEntry = TransactionLogEntry.getNextTransactionLogEntry(logFC,
                        logFC.position(), false);
            } catch (EOFException eofe) {
                return;
            }
            if (logEntry == null) {
                continue;
            }
            byte operationType = logEntry.getOperationType();
            XidImpl xid = logEntry.getXid();
            switch (operationType) {
                case TransactionLogEntry.COMMIT_BEGINS:
                    onePhaseCommittingTransactions.add(xid);
                    preparedInDoubtTransactions.remove(xid);
                    heavyWriteTransactionsForRollback.remove(xid);
                    eventsDequeuePrepared.remove(xid);
                    break;
                case TransactionLogEntry.TXN_COMMIT_DONE:
                    onePhaseCommittingTransactions.remove(xid);
                    preparedInDoubtTransactions.remove(xid);
                    heavyWriteTransactionsForRollback.remove(xid);
                    committedTransactions.add(xid);
                    eventsDequeuePrepared.remove(xid);
                    ArrayList<FileStateChangeEvent> events = eventsEnqueuePreparedOnly.remove(xid);
                    if (events != null) {
                        eventsEnqueueCommittedNotDequeued.addAll(events);
                    }
                    break;
                case TransactionLogEntry.TXN_ROLLBACK_DONE:
                    onePhaseCommittingTransactions.remove(xid);
                    preparedInDoubtTransactions.remove(xid);
                    heavyWriteTransactionsForRollback.remove(xid);
                    eventsDequeuePrepared.remove(xid);
                    eventsEnqueuePreparedOnly.remove(xid);
                    break;
                case TransactionLogEntry.PREPARE_COMPLETES:
                    preparedInDoubtTransactions.add(xid);
                    heavyWriteTransactionsForRollback.remove(xid);
                    break;
                case TransactionLogEntry.TXN_USES_UNDO_LOGS:
                    heavyWriteTransactionsForRollback.add(xid);
                    break;
                case TransactionLogEntry.EVENT_ENQUEUE:
                    eventsEnqueuePreparedOnly.put(xid, logEntry.getEventList());
                    break;
                case TransactionLogEntry.EVENT_DEQUEUE:
                    eventsDequeueCommitted.add(logEntry.getEventList().get(0));
                    break;
                case TransactionLogEntry.PREPARE_COMPLETES_FOR_EVENT_DEQUEUE:
                    eventsDequeuePrepared.put(xid, logEntry.getEventList().get(0));
                    break;
                case TransactionLogEntry.REMOTE_ENDPOINT_ACTIVATES:
                    //we need not preserve the txn logs for these entries because we are now calling
                    //the epActivation again, so another log will get created.
                    remoteActivations.add(logEntry.getRemoteActivation());
                    break;
                case TransactionLogEntry.REMOTE_ENDPOINT_DEACTIVATES:
                    remoteActivations.remove(logEntry.getRemoteActivation());
                    break;
            }
        }
    }

    private void collectTransactionLogPositions(FileChannel logFC, int logIndex)
            throws IOException {
        TransactionLogEntry logEntry = null;
        while (true) {
            long filePositionAtBuffersBeginning = logFC.position();
            try {
                logEntry = TransactionLogEntry.getNextTransactionLogEntry(logFC,
                        filePositionAtBuffersBeginning, false);
            } catch (EOFException oefe) {
                return;
            }
            XidImpl xid = logEntry.getXid();
            if (onePhaseCommittingTransactions.contains(xid) || preparedInDoubtTransactions.contains(xid)) {
                if (logEntry.isRedoLogEntry() || logEntry.isUndoLogEntry()) {
                    addLogPositionToTransaction(xid, logIndex, filePositionAtBuffersBeginning);
                }
                if (logEntry.getOperationType() == TransactionLogEntry.CHECKPOINT_AVOIDING_COPY_OR_MOVE_REDO) {
                    transactionsLatestCheckPoint.put(xid, logEntry.getCheckPointPosition());
                }
                if (logEntry.getOperationType() == TransactionLogEntry.FILES_ALREADY_ONDISK) {
                    transactionsAndFilesWithLatestViewOnDisk.put(xid, logEntry.getFileList());
                }
            }
            if (heavyWriteTransactionsForRollback.contains(xid)) {
                if (logEntry.isUndoLogEntry()) {
                    addLogPositionToTransaction(xid, logIndex, filePositionAtBuffersBeginning);
                }
            }
        }
    }

    private void registerRemoteEndpoints() throws IOException {
        for (EndPointActivation activation : remoteActivations) {
            xaFileSystem.registerEndPointActivation(activation);
        }
    }

    private void recoverOnePhaseTransactions() throws Exception {
        WorkManager workManager = NativeXAFileSystem.getWorkManager();
        for (XidImpl xid : onePhaseCommittingTransactions) {
            if (released) {
                return;
            }
            NativeSession recoverySession;
            ArrayList<FileStateChangeEvent> events = getEventsFromPreparedTransaction(xid);
            recoverySession = xaFileSystem.createRecoverySession(xid, events);

            TransactionCompleter commitWork = new TransactionCompleter(recoverySession, this, true);
            workManager.startWork(commitWork);
        }
    }

    private void recoverHeavyWriteTransactionsForRollback() throws Exception {
        WorkManager workManager = NativeXAFileSystem.getWorkManager();
        for (XidImpl xid : heavyWriteTransactionsForRollback) {
            if (released) {
                return;
            }
            NativeSession recoverySession = xaFileSystem.createRecoverySession(xid, null);
            TransactionCompleter rollbackWork = new TransactionCompleter(recoverySession, this, false);
            workManager.startWork(rollbackWork);
        }
    }

    public ArrayList<FileStateChangeEvent> getEventsFromPreparedTransaction(XidImpl xid) {
        return eventsEnqueuePreparedOnly.get(xid);
    }

    private void prepareEventsToPopulate() {
        ArrayList<FileStateChangeEvent> eventsNotToPopulate = new ArrayList<FileStateChangeEvent>();
        eventsNotToPopulate.addAll(eventsDequeueCommitted);
        eventsNotToPopulate.addAll(eventsDequeuePrepared.values());
        eventsEnqueueCommittedNotDequeued.removeAll(eventsNotToPopulate);
    }

    public ArrayList<FileStateChangeEvent> getEventsEnqueueCommittedNotDequeued() {
        return eventsEnqueueCommittedNotDequeued;
    }

    public void cleanupTransactionInfo(XidImpl xid) throws IOException {
        distanceFromRecoveryCompletion.decrementAndGet();
        checkForRecoveryDone();
    }

    private void addLogPositionToTransaction(XidImpl xid, int logFileIndex, long localPosition) {
        TransactionLogsUtility.addLogPositionToTransaction(xid, logFileIndex, localPosition, transactionLogPositions);
    }

    public ArrayList<Long> getTransactionLogsPositions(XidImpl xid) {
        ArrayList<Long> logPositions = transactionLogPositions.get(xid);
        if (logPositions == null) {
            return new ArrayList<Long>(0);
        }
        return logPositions;
    }

    public int getTransactionsLatestCheckPoint(XidImpl xid) {
        Integer latestCheckPoint = transactionsLatestCheckPoint.get(xid);
        return latestCheckPoint == null ? -1 : latestCheckPoint;
    }

    public HashSet<XidImpl> getPreparedInDoubtTransactions() {
        return preparedInDoubtTransactions;
    }

    public HashMap<XidImpl, FileStateChangeEvent> getPreparedInDoubtTransactionsOfDequeue() {
        return eventsDequeuePrepared;
    }

    public HashSet getFilesOnDiskForTransaction(XidImpl xid) {
        return transactionsAndFilesWithLatestViewOnDisk.get(xid);
    }

    private class TransactionCompleter implements Work {

        private NativeSession session;
        private final CrashRecoveryWorker crashRecoveryWorker;
        private boolean toCommit;

        private TransactionCompleter(NativeSession session, CrashRecoveryWorker crashRecoveryWorker, boolean toCommit) {
            this.session = session;
            this.crashRecoveryWorker = crashRecoveryWorker;
            this.toCommit = toCommit;
        }

        public void release() {
        }

        public void run() {
            try {
                if (toCommit) {
                    session.commit(true);
                } else {
                    session.rollback();
                }
            } catch (TransactionRolledbackException trbe) {
            }
        }
    }
}

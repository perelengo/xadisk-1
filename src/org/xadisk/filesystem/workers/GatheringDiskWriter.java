package org.xadisk.filesystem.workers;

import org.xadisk.filesystem.pools.PooledBuffer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.xadisk.connector.inbound.EndPointActivation;
import org.xadisk.filesystem.Buffer;
import org.xadisk.filesystem.FileStateChangeEvent;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.OnDiskInfo;
import org.xadisk.filesystem.TransactionLogEntry;
import org.xadisk.filesystem.XidImpl;
import org.xadisk.filesystem.utilities.TransactionLogsUtility;

public class GatheringDiskWriter extends EventWorker {

    private final int cumulativeBufferSizeForDiskWrite;
    private final AtomicInteger cumulativeBufferSize = new AtomicInteger(0);
    private FileChannel transactionLogChannel;
    private final ConcurrentHashMap<XidImpl, ArrayList<Buffer>> transactionSubmittedBuffers =
            new ConcurrentHashMap<XidImpl, ArrayList<Buffer>>(1000);
    private final NativeXAFileSystem theXAFileSystem;
    private final long transactionLogFileMaxSize;
    private final ReentrantLock transactionLogLock = new ReentrantLock(false);
    private final String transactionLogBaseName;
    private int currentLogIndex;
    private final HashMap<Integer, Integer> transactionLogsAndOpenTransactions = new HashMap<Integer, Integer>(2);
    private final HashMap<XidImpl, ArrayList<Integer>> transactionsAndLogsOccupied = new HashMap<XidImpl, ArrayList<Integer>>(1000);
    private final long maxNonPooledBufferSize;

    public GatheringDiskWriter(int cumulativeBufferSizeForDiskWrite, long transactionLogFileMaxSize,
            long maxNonPooledBufferSize,
            String transactionLogBaseName, NativeXAFileSystem theXAFileSystem)
            throws IOException {
        this.cumulativeBufferSizeForDiskWrite = cumulativeBufferSizeForDiskWrite;
        this.theXAFileSystem = theXAFileSystem;
        this.transactionLogFileMaxSize = transactionLogFileMaxSize;
        this.transactionLogBaseName = transactionLogBaseName;
        this.maxNonPooledBufferSize = maxNonPooledBufferSize;
    }

    public void initialize() throws IOException {
        File currentTransactionLog = null;
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            File temp = new File(transactionLogBaseName + "_" + i);
            if (temp.exists()) {
                continue;
            }
            currentTransactionLog = temp;
            this.currentLogIndex = i;
            break;
        }
        if (currentTransactionLog == null) {
            throw new IOException("System has reached its limit on number of transaction logs.");
        }
        this.transactionLogChannel = new FileOutputStream(currentTransactionLog, false).getChannel();
    }

    public void deInitialize() throws IOException {
        this.transactionLogChannel.close();
    }

    @Override
    void processEvent() {
        try {
            Buffer buffersArray[];
            XidImpl xids[];
            ArrayList<Buffer> allBuffersToWrite = new ArrayList<Buffer>(1000);
            ArrayList<XidImpl> xidsList = new ArrayList<XidImpl>(1000);
            Iterator<Map.Entry<XidImpl, ArrayList<Buffer>>> entries = transactionSubmittedBuffers.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<XidImpl, ArrayList<Buffer>> entry = entries.next();
                XidImpl xid = entry.getKey();
                ArrayList<Buffer> txnBuffers = transactionSubmittedBuffers.put(xid, new ArrayList<Buffer>(10));
                for (Buffer buffer : txnBuffers) {
                    xidsList.add(xid);
                    allBuffersToWrite.add(buffer);
                }
            }
            xids = xidsList.toArray(new XidImpl[0]);
            buffersArray = allBuffersToWrite.toArray(new Buffer[0]);
            try {
                transactionLogLock.lock();
                writeBuffersToTransactionLog(buffersArray, xids, 0);
            } finally {
                transactionLogLock.unlock();
            }
        } catch (Throwable t) {
            theXAFileSystem.notifySystemFailure(t);
        }
    }

    public void writeRemainingBuffersNow(XidImpl xid) throws IOException {
        ArrayList<Buffer> txnBuffers = transactionSubmittedBuffers.put(xid, new ArrayList<Buffer>(10));
        for (Buffer buffer : txnBuffers) {
            cumulativeBufferSize.getAndAdd(-buffer.getBuffer().remaining());
        }
        try {
            XidImpl xids[] = new XidImpl[txnBuffers.size()];
            for (int i = 0; i < xids.length; i++) {
                xids[i] = xid;
            }
            try {
                transactionLogLock.lock();
                writeBuffersToTransactionLog(txnBuffers.toArray(new Buffer[0]), xids, 0);
            } finally {
                transactionLogLock.unlock();
            }
        } catch (IOException ioe) {
            theXAFileSystem.notifySystemFailure(ioe);
        }
    }

    private void writeBuffersToTransactionLog(Buffer buffersArray[], XidImpl xids[], int offset) throws IOException {
        ByteBuffer byteBufferArray[] = new ByteBuffer[buffersArray.length];
        long sizeToWriteNow = 0;
        int canProcessTill = buffersArray.length - 1;
        for (int i = offset; i < buffersArray.length; i++) {
            byteBufferArray[i] = buffersArray[i].getBuffer();
            if (sizeToWriteNow + byteBufferArray[i].remaining() > transactionLogFileMaxSize) {
                canProcessTill = i - 1;
                break;
            }
            sizeToWriteNow += byteBufferArray[i].remaining();
        }
        ensureLogFileCapacity(sizeToWriteNow);
        long entryPosition = transactionLogChannel.position();
        ArrayList<Integer> buffersToMakeOnDisk = new ArrayList<Integer>(1000);
        boolean makeCurrentOnDisk;
        for (int i = offset; i <= canProcessTill; i++) {
            byteBufferArray[i] = buffersArray[i].getBuffer();
            if (buffersArray[i] instanceof PooledBuffer) {
                makeCurrentOnDisk = false;
            } else {
                if (Buffer.getTotalNonPooledBufferSize() < maxNonPooledBufferSize * 3 / 4) {
                    makeCurrentOnDisk = false;
                } else if (Buffer.getTotalNonPooledBufferSize() < maxNonPooledBufferSize) {
                    if (byteBufferArray[i].remaining() < 1000) {
                        makeCurrentOnDisk = false;
                    } else {
                        makeCurrentOnDisk = true;
                    }
                } else {
                    makeCurrentOnDisk = true;
                }
            }
            if (makeCurrentOnDisk) {
                addLogPositionToTransaction(xids[i], currentLogIndex, entryPosition);
                buffersToMakeOnDisk.add(i);
                buffersArray[i].setOnDiskInfo(new OnDiskInfo(currentLogIndex, entryPosition));
            } else {
                addInMemoryBufferToTransaction(xids[i], buffersArray[i]);
            }
            entryPosition += byteBufferArray[i].remaining();
        }
        long n = 0;
        while (n < sizeToWriteNow) {
            n += transactionLogChannel.write(byteBufferArray, offset, canProcessTill - offset + 1);
        }

        for (Integer indices : buffersToMakeOnDisk) {
            Buffer temp = buffersArray[indices];
            temp.makeOnDisk(temp.getOnDiskInfo());
        }

        if (canProcessTill < buffersArray.length - 1) {
            writeBuffersToTransactionLog(buffersArray, xids, canProcessTill + 1);
        }
    }

    private void addLogPositionToTransaction(XidImpl xid, int logFileIndex, long localPosition) {
        xid.getOwningSession().addLogPositionToTransaction(logFileIndex, localPosition);
        TransactionLogsUtility.trackTransactionLogsUsage(xid, transactionsAndLogsOccupied, transactionLogsAndOpenTransactions, logFileIndex);
    }

    private void addInMemoryBufferToTransaction(XidImpl xid, Buffer buffer) {
        xid.getOwningSession().addInMemoryBufferToTransaction(buffer);
    }

    public void submitBuffer(Buffer logEntry, XidImpl xid) {
        logEntry.flushByteBufferChanges();
        ArrayList<Buffer> txnBuffers = transactionSubmittedBuffers.get(xid);
        if (txnBuffers == null) {
            txnBuffers = new ArrayList<Buffer>(10);
            txnBuffers.add(logEntry);
            transactionSubmittedBuffers.put(xid, txnBuffers);
        } else {
            txnBuffers.add(logEntry);
        }
        int currentCumulativeSize = cumulativeBufferSize.addAndGet(logEntry.getBuffer().remaining());
        raiseEventThreadSafely(currentCumulativeSize);
    }

    private void raiseEventThreadSafely(int currentCumulativeSize) {
        if (currentCumulativeSize >= cumulativeBufferSizeForDiskWrite) {
            while (!cumulativeBufferSize.compareAndSet(currentCumulativeSize, 0)) {
                currentCumulativeSize = cumulativeBufferSize.get();
                if (currentCumulativeSize < cumulativeBufferSizeForDiskWrite) {
                    return;
                }
            }
            raiseEvent();
        }
    }

    public void transactionCommitBegins(XidImpl xid) throws IOException {
        ByteBuffer temp = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid,
                TransactionLogEntry.COMMIT_BEGINS));
        forceWrite(temp);
    }

    public void transactionCompletes(XidImpl xid, boolean isCommitted) throws IOException {
        ByteBuffer temp = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, isCommitted ? TransactionLogEntry.TXN_COMMIT_DONE : TransactionLogEntry.TXN_ROLLBACK_DONE));
        forceWrite(temp);
    }

    public void transactionPrepareCompletes(XidImpl xid) throws IOException {
        ByteBuffer temp = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid,
                TransactionLogEntry.PREPARE_COMPLETES));
        forceWrite(temp);
    }

    public void transactionPrepareCompletesForEventDequeue(XidImpl xid, FileStateChangeEvent event) throws IOException {
        ArrayList<FileStateChangeEvent> events = new ArrayList<FileStateChangeEvent>(1);
        events.add(event);
        ByteBuffer temp = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, events,
                TransactionLogEntry.PREPARE_COMPLETES_FOR_EVENT_DEQUEUE));
        forceWrite(temp);
    }

    public void recordEndPointActivation(EndPointActivation activation) throws IOException {
        ByteBuffer temp = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(activation,
                TransactionLogEntry.REMOTE_ENDPOINT_ACTIVATES));
        forceWrite(temp);
    }

    public void recordEndPointDeActivation(EndPointActivation activation) throws IOException {
        ByteBuffer temp = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(activation,
                TransactionLogEntry.REMOTE_ENDPOINT_DEACTIVATES));
        forceWrite(temp);
    }

    public void forceLog(ByteBuffer logEntryHeader) throws IOException {
        try {
            transactionLogLock.lock();
            long n = 0;
            long sizeToWrite = logEntryHeader.remaining();
            ensureLogFileCapacity(sizeToWrite);
            while (n < sizeToWrite) {
                n += transactionLogChannel.write(logEntryHeader);
            }
            transactionLogChannel.force(false);
        } finally {
            transactionLogLock.unlock();
        }
    }

    public long[] forceUndoLogAndData(XidImpl xid, ByteBuffer logEntryHeader, FileChannel contents, long contentPosition,
            long contentLength)
            throws IOException {
        long logPosition[] = new long[2];
        try {
            transactionLogLock.lock();
            long n = 0;
            long headerSize = logEntryHeader.remaining();
            long totalLogSizeRequiredForThisRequest = headerSize;
            if (contentLength > 0) {
                totalLogSizeRequiredForThisRequest += contentLength;
            }
            ensureLogFileCapacity(totalLogSizeRequiredForThisRequest);
            logPosition[0] = currentLogIndex;
            logPosition[1] = transactionLogChannel.position();
            while (n < headerSize) {
                n += transactionLogChannel.write(logEntryHeader);
            }
            if (contentLength > 0) {
                n = 0;
                long startingPosition = transactionLogChannel.position();
                contents.position(contentPosition);
                while (n < contentLength) {
                    n += transactionLogChannel.transferFrom(contents, startingPosition + n, contentLength - n);
                }
                transactionLogChannel.position(startingPosition + n);
            }
            transactionLogChannel.force(false);
            addLogPositionToTransaction(xid, (int) logPosition[0], logPosition[1]);
            return logPosition;
        } finally {
            transactionLogLock.unlock();
        }
    }

    private void forceWrite(ByteBuffer buffer) throws IOException {
        try {
            transactionLogLock.lock();
            long n = 0;
            long sizeToWrite = buffer.remaining();
            ensureLogFileCapacity(sizeToWrite);
            while (n < sizeToWrite) {
                n += transactionLogChannel.write(buffer);
            }
            transactionLogChannel.force(false);
        } finally {
            transactionLogLock.unlock();
        }
    }

    public void cleanupTransactionInfo(XidImpl xid) throws IOException {
        try {
            transactionLogLock.lock();
            TransactionLogsUtility.deleteLogsIfPossible(xid, transactionsAndLogsOccupied, transactionLogsAndOpenTransactions, currentLogIndex, transactionLogBaseName);
        } finally {
            transactionLogLock.unlock();
        }
    }

    private void ensureLogFileCapacity(long sizeToWriteNow) throws IOException {
        if (transactionLogChannel.size() + sizeToWriteNow > transactionLogFileMaxSize) {
            File nextTransactionLog = null;
            for (int i = currentLogIndex + 1; i < Integer.MAX_VALUE; i++) {
                File f = new File(transactionLogBaseName + "_" + i);
                if (f.exists()) {
                    continue;
                }
                nextTransactionLog = f;
                transactionLogChannel.close();
                transactionLogChannel =
                        new FileOutputStream(nextTransactionLog, true).getChannel();
                currentLogIndex = i;
                recordAllActivationsInNewLog();
                break;
            }
            if (nextTransactionLog == null) {
                throw new IOException("Transaction logs seems to be over...cannot proceed.");
            }
        }
    }

    private void recordAllActivationsInNewLog() throws IOException {
        for (EndPointActivation activation : theXAFileSystem.getAllActivations()) {
            //note that this solution means duplicate entries for a single activation; eg one entry
            //in log 3 and other in 4 when log 4 is allocated. The other solution where we
            //keep entries as such and carry-them-over during log deletion is not full-proof
            //because it also means a crash can happen during transfer of entries from log-be-deleted
            //to current log and this implies duplicate entries or absent entries.
            //With the current solution though, we will have to be careful during activations
            //after recovery to avoid duplicates.
            //Current solution is safe because it works even for a case when a log get deleted
            //because we never delete the "current" log and we can see that
            //during current log creation we save all activation "during" that time, implying
            //that it also includes activations in the log we are deleting because
            //once the current log got created all activation onwards get anyway persisted in the
            //current log.
            recordEndPointActivation(activation);
        }
    }
}

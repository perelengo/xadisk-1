package org.xadisk.connector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.filesystem.FileStateChangeEvent;
import org.xadisk.filesystem.TransactionLogEntry;
import org.xadisk.filesystem.XAFileSystem;
import org.xadisk.filesystem.XidImpl;

public class EventProcessingXAResource implements XAResource {

    private final ConcurrentHashMap<Xid, XidImpl> internalXids = new ConcurrentHashMap<Xid, XidImpl>(1000);
    private final XAFileSystem xaFileSystem;
    private final FileStateChangeEvent event;
    private volatile boolean returnedAllPreparedTransactions = false;
    private final boolean isCreatedForRecovery;
    private volatile HashMap<XidImpl, FileStateChangeEvent> dequeuingTransactionsPreparedPreCrash;

    public EventProcessingXAResource(XAFileSystem xaFileSystem, FileStateChangeEvent event) {
        this.xaFileSystem = xaFileSystem;
        this.event = event;
        this.isCreatedForRecovery = false;
    }

    public EventProcessingXAResource(XAFileSystem xaFileSystem) {
        this.xaFileSystem = xaFileSystem;
        this.isCreatedForRecovery = true;
        this.event = null;
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        XidImpl xidImpl = mapToInternalXid(xid);
        if (flags == XAResource.TMNOFLAGS) {
        } else {
            //unexpected.
        }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        XidImpl xidImpl = mapToInternalXid(xid);
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        XidImpl xidImpl = mapToInternalXid(xid);
        if (isCreatedForRecovery) {
            //not expected.
        }
        try {
            xaFileSystem.getTheGatheringDiskWriter().transactionPrepareCompletesForEventDequeue(xidImpl, event);
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailureAndContinue(ioe);
            throw new XAException(XAException.XAER_RMFAIL);
        }
        return XAResource.XA_OK;
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        XidImpl xidImpl = mapToInternalXid(xid);
        FileStateChangeEvent eventForTransaction = null;
        try {
            if (isCreatedForRecovery) {
                eventForTransaction = dequeuingTransactionsPreparedPreCrash.get(xidImpl);
            } else {
                eventForTransaction = this.event;
            }
            ArrayList<FileStateChangeEvent> events = new ArrayList<FileStateChangeEvent>(1);
            events.add(eventForTransaction);
            ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xidImpl, events,
                    TransactionLogEntry.EVENT_DEQUEUE));
            xaFileSystem.getTheGatheringDiskWriter().forceLog(logEntryBytes);
            xaFileSystem.getTheGatheringDiskWriter().transactionCompletes(xidImpl, true);
            if (isCreatedForRecovery) {
                xaFileSystem.getRecoveryWorker().cleanupTransactionInfo(xidImpl);
            }
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailureAndContinue(ioe);
            throw new XAException(XAException.XAER_RMFAIL);
        } finally {
            releaseFromInternalXidMap(xid);
        }
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        XidImpl xidImpl = mapToInternalXid(xid);
        try {
            if (isCreatedForRecovery) {
                FileStateChangeEvent temp = dequeuingTransactionsPreparedPreCrash.get(xidImpl);
            }
            xaFileSystem.getTheGatheringDiskWriter().transactionCompletes(xidImpl, false);
            if (isCreatedForRecovery) {
                xaFileSystem.getRecoveryWorker().cleanupTransactionInfo(xidImpl);
            }
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailureAndContinue(ioe);
            throw new XAException(XAException.XAER_RMFAIL);
        } finally {
            releaseFromInternalXidMap(xid);
        }
    }

    @Override
    public void forget(Xid xid) throws XAException {
        XidImpl xidImpl = mapToInternalXid(xid);
    }
    
    @Override
    public Xid[] recover(int flag) throws XAException {
        if (!xaFileSystem.getRecoveryWorker().isRecoveryDataCollectionDone()) {
            throw new XAException(XAException.XAER_RMFAIL);
        }
        if (flag == XAResource.TMSTARTRSCAN) {
            returnedAllPreparedTransactions = false;
        }
        if (flag == (XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN)) {
            returnedAllPreparedTransactions = false;
        }
        if (returnedAllPreparedTransactions) {
            return new Xid[0];
        }

        dequeuingTransactionsPreparedPreCrash = xaFileSystem.getRecoveryWorker().
                getPreparedInDoubtTransactionsForDequeue();

        Xid xids[];
        xids = dequeuingTransactionsPreparedPreCrash.keySet().toArray(new Xid[0]);
        returnedAllPreparedTransactions = true;
        return xids;
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    @Override
    public boolean setTransactionTimeout(int arg0) throws XAException {
        return false;
    }

    @Override
    public boolean isSameRM(XAResource obj) throws XAException {
        if (obj instanceof EventProcessingXAResource) {
            return true;
        }
        return false;
    }

    private XidImpl mapToInternalXid(Xid xid) {
        synchronized (internalXids) {
            XidImpl internalXid = internalXids.get(xid);
            if (internalXid == null) {
                internalXid = new XidImpl(xid);
                internalXids.put(xid, internalXid);
            }
            return internalXid;
        }
    }
    
    private void releaseFromInternalXidMap(Xid xid) {
        internalXids.remove(xid);
    }
}

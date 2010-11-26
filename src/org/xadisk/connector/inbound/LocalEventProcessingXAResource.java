package org.xadisk.connector.inbound;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.Status;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.filesystem.FileStateChangeEvent;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.TransactionLogEntry;
import org.xadisk.filesystem.XidImpl;

public class LocalEventProcessingXAResource implements XAResource {

    private final ConcurrentHashMap<Xid, XidImpl> internalXids = new ConcurrentHashMap<Xid, XidImpl>(1000);
    private final Object lockOnInternalXids = new ArrayList<Object>(0);
    private final NativeXAFileSystem xaFileSystem;
    private final FileStateChangeEvent event;
    private volatile boolean returnedAllPreparedTransactions = false;
    private final boolean isCreatedForRecovery;
    private volatile HashMap<XidImpl, FileStateChangeEvent> dequeuingTransactionsPreparedPreCrash;
    private byte transactionOutcome = Status.STATUS_NO_TRANSACTION;

    public LocalEventProcessingXAResource(NativeXAFileSystem xaFileSystem, FileStateChangeEvent event) {
        this.xaFileSystem = xaFileSystem;
        this.event = event;
        this.isCreatedForRecovery = false;
    }

    public LocalEventProcessingXAResource(NativeXAFileSystem xaFileSystem) {
        this.xaFileSystem = xaFileSystem;
        this.isCreatedForRecovery = true;
        this.event = null;
    }

    public void start(Xid xid, int flags) throws XAException {
        //XidImpl xidImpl = mapToInternalXid(xid);
        if (flags == XAResource.TMNOFLAGS) {
        } else {
            //unexpected.
        }
    }

    public void end(Xid xid, int flags) throws XAException {
        //XidImpl xidImpl = mapToInternalXid(xid);
    }

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
            this.transactionOutcome = Status.STATUS_COMMITTED;
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailureAndContinue(ioe);
            throw new XAException(XAException.XAER_RMFAIL);
        } finally {
            releaseFromInternalXidMap(xid);
        }
    }

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
            this.transactionOutcome = Status.STATUS_ROLLEDBACK;
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailureAndContinue(ioe);
            throw new XAException(XAException.XAER_RMFAIL);
        } finally {
            releaseFromInternalXidMap(xid);
        }
    }

    public void forget(Xid xid) throws XAException {
        //XidImpl xidImpl = mapToInternalXid(xid);
    }

    public Xid[] recover(int flag) throws XAException {
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
                getPreparedInDoubtTransactionsOfDequeue();

        Xid xids[];
        xids = dequeuingTransactionsPreparedPreCrash.keySet().toArray(new Xid[0]);
        returnedAllPreparedTransactions = true;
        return xids;
    }

    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    public boolean setTransactionTimeout(int arg0) throws XAException {
        return false;
    }

    public boolean isSameRM(XAResource obj) throws XAException {
        //never ever return true : note that this object is use-once; mainly the
        //"event" object is being used in prepare/commit; so imagine the disaster
        //if the TM keeps calling commit on the same XAR because it thinks that
        //the 2 local XARs are same ; though they are actually same "RM" here but
        //the consequence is that the "call commit on any of the sameRM xar" created
        //problem.
        return false;
    }

    private XidImpl mapToInternalXid(Xid xid) {
        synchronized (lockOnInternalXids) {
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

    public byte getTransactionOutcome() {
        return transactionOutcome;
    }
}

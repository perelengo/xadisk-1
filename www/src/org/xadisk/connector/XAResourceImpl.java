package org.xadisk.connector;

import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.filesystem.Session;
import org.xadisk.filesystem.XAFileSystem;
import org.xadisk.filesystem.XidImpl;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;
import org.xadisk.filesystem.exceptions.XASystemException;

public class XAResourceImpl implements XAResource {

    private final ManagedConnectionImpl mc;
    private final XAFileSystem xaFileSystem;
    private final static ConcurrentHashMap<Xid, XidImpl> internalXids = new ConcurrentHashMap<Xid, XidImpl>(1000);
    private volatile int transactionTimeout;

    XAResourceImpl(ManagedConnectionImpl mc) {
        this.mc = mc;
        this.xaFileSystem = XAFileSystem.getXAFileSystem();
        transactionTimeout = xaFileSystem.getDefaultTransactionTimeout();
    }

    @Override
    public void start(Xid xid, int flag) throws XAException {
        XidImpl internalXid = mapToInternalXid(xid);
        if (flag == XAResource.TMNOFLAGS) {
            try {
                Session session = mc.refreshSessionForNewXATransaction(internalXid);
                session.setTransactionTimeout(transactionTimeout);
            } catch (XASystemException xase) {
                throw new XAException(XAException.XAER_RMFAIL);
            }
            mc.setTypeOfOngoingTransaction(ManagedConnectionImpl.XA_TRANSACTION);
        }
        if (flag == XAResource.TMJOIN) {
            Session sessionOfTransaction = xaFileSystem.getSessionForTransaction(internalXid);
            if (sessionOfTransaction == null) {
                throw new XAException(XAException.XAER_INVAL);
            }
            mc.setSessionOfExistingXATransaction(sessionOfTransaction);
            mc.setTypeOfOngoingTransaction(ManagedConnectionImpl.XA_TRANSACTION);
        }
        if (flag == XAResource.TMRESUME) {
            Session sessionOfTransaction = xaFileSystem.getSessionForTransaction(internalXid);
            if (sessionOfTransaction == null) {
                throw new XAException(XAException.XAER_INVAL);
            }
            mc.setSessionOfExistingXATransaction(sessionOfTransaction);
            mc.setTypeOfOngoingTransaction(ManagedConnectionImpl.XA_TRANSACTION);
        }
    }

    @Override
    public void end(Xid xid, int flag) throws XAException {
        XidImpl internalXid = mapToInternalXid(xid);
        Session sessionOfTransaction = xaFileSystem.getSessionForTransaction(internalXid);
        if (sessionOfTransaction == null) {
            throw new XAException(XAException.XAER_INVAL);
        }
        if (flag == XAResource.TMSUCCESS) {
            mc.setTypeOfOngoingTransaction(ManagedConnectionImpl.NO_TRANSACTION);
        }
        if (flag == XAResource.TMFAIL) {
            mc.setTypeOfOngoingTransaction(ManagedConnectionImpl.NO_TRANSACTION);
        }
        if (flag == XAResource.TMSUSPEND) {
            mc.setTypeOfOngoingTransaction(ManagedConnectionImpl.NO_TRANSACTION);
        }
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        XidImpl internalXid = mapToInternalXid(xid);
        Session sessionOfTransaction = xaFileSystem.getSessionForTransaction(internalXid);
        if (sessionOfTransaction == null) {
            throw new XAException(XAException.XAER_INVAL);
        }
        try {
            sessionOfTransaction.prepare();
            return XAResource.XA_OK;
        } catch (TransactionRolledbackException trbe) {
            releaseFromInternalXidMap(xid);
            throw new XAException(XAException.XA_RBROLLBACK);
        } catch (XASystemException xase) {
            releaseFromInternalXidMap(xid);
            throw new XAException(XAException.XAER_RMFAIL);
        }
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        XidImpl internalXid = mapToInternalXid(xid);
        Session sessionOfTransaction = xaFileSystem.getSessionForTransaction(internalXid);
        if (sessionOfTransaction == null) {
            throw new XAException(XAException.XAER_INVAL);
        }
        try {
            sessionOfTransaction.rollback();
        } catch (TransactionRolledbackException trbe) {
            throw new XAException(XAException.XA_RBROLLBACK);
        } catch (XASystemException xase) {
            throw new XAException(XAException.XAER_RMFAIL);
        } finally {
            releaseFromInternalXidMap(xid);
        }
        mc.setTypeOfOngoingTransaction(ManagedConnectionImpl.NO_TRANSACTION);
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        XidImpl internalXid = mapToInternalXid(xid);
        Session sessionOfTransaction = xaFileSystem.getSessionForTransaction(internalXid);
        if (sessionOfTransaction == null) {
            throw new XAException(XAException.XAER_INVAL);
        }
        try {
            sessionOfTransaction.commit(onePhase);
        } catch (TransactionRolledbackException trbe) {
            throw new XAException(XAException.XA_RBROLLBACK);
        } catch (XASystemException xase) {
            throw new XAException(XAException.XAER_RMFAIL);
        } finally {
            releaseFromInternalXidMap(xid);
        }
        mc.setTypeOfOngoingTransaction(ManagedConnectionImpl.NO_TRANSACTION);
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        return xaFileSystem.recover(flag);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        Xid internalXid = mapToInternalXid(xid);
    }

    @Override
    public boolean isSameRM(XAResource xar) throws XAException {
        if (xar instanceof XAResourceImpl) {
            return true;
        }
        return false;
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return transactionTimeout;
    }

    @Override
    public boolean setTransactionTimeout(int transactionTimeout) throws XAException {
        this.transactionTimeout = transactionTimeout;
        return true;
    }

    private XidImpl mapToInternalXid(Xid xid) {
        XidImpl internalXid = internalXids.get(xid);
        if (internalXid == null) {
            internalXid = new XidImpl(xid);
            internalXids.put(xid, internalXid);
        }
        return internalXid;
    }

    private void releaseFromInternalXidMap(Xid xid) {
        internalXids.remove(xid);
    }
}

package org.xadisk.connector;

import org.xadisk.connector.outbound.XADiskManagedConnection;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.Session;
import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.bridge.proxies.impl.XADiskRemoteManagedConnection;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.XidImpl;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;
import org.xadisk.filesystem.exceptions.XASystemException;

public class XAResourceImpl implements XAResource {

    private final XADiskManagedConnection mc;
    private final XAFileSystem xaFileSystem;
    private final static ConcurrentHashMap<Xid, XidImpl> internalXids = new ConcurrentHashMap<Xid, XidImpl>(1000);
    private volatile int transactionTimeout;

    public XAResourceImpl(XADiskManagedConnection mc) {
        this.mc = mc;
        this.xaFileSystem = mc.getTheUnderlyingXAFileSystem();
        transactionTimeout = xaFileSystem.getDefaultTransactionTimeout();
    }

    public void start(Xid xid, int flag) throws XAException {
        XidImpl internalXid = mapToInternalXid(xid);
        if (flag == XAResource.TMNOFLAGS) {
            try {
                Session session = mc.refreshSessionForNewXATransaction(internalXid);
                session.setTransactionTimeout(transactionTimeout);
            } catch (XASystemException xase) {
                throw new XAException(XAException.XAER_RMFAIL);
            }
            mc.setTypeOfOngoingTransaction(XADiskManagedConnection.XA_TRANSACTION);
        }
        if (flag == XAResource.TMJOIN) {
            Session sessionOfTransaction = xaFileSystem.getSessionForTransaction(internalXid);
            if (sessionOfTransaction == null) {
                throw new XAException(XAException.XAER_INVAL);
            }
            mc.setSessionOfExistingXATransaction(sessionOfTransaction);
            mc.setTypeOfOngoingTransaction(XADiskManagedConnection.XA_TRANSACTION);
        }
        if (flag == XAResource.TMRESUME) {
            Session sessionOfTransaction = xaFileSystem.getSessionForTransaction(internalXid);
            if (sessionOfTransaction == null) {
                throw new XAException(XAException.XAER_INVAL);
            }
            mc.setSessionOfExistingXATransaction(sessionOfTransaction);
            mc.setTypeOfOngoingTransaction(XADiskManagedConnection.XA_TRANSACTION);
        }
    }

    public void end(Xid xid, int flag) throws XAException {
        XidImpl internalXid = mapToInternalXid(xid);
        Session sessionOfTransaction = xaFileSystem.getSessionForTransaction(internalXid);
        if (sessionOfTransaction == null) {
            throw new XAException(XAException.XAER_INVAL);
        }
        if (flag == XAResource.TMSUCCESS) {
            mc.setTypeOfOngoingTransaction(XADiskManagedConnection.NO_TRANSACTION);
        }
        if (flag == XAResource.TMFAIL) {
            mc.setTypeOfOngoingTransaction(XADiskManagedConnection.NO_TRANSACTION);
        }
        if (flag == XAResource.TMSUSPEND) {
            mc.setTypeOfOngoingTransaction(XADiskManagedConnection.NO_TRANSACTION);
        }
    }

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
        mc.setTypeOfOngoingTransaction(XADiskManagedConnection.NO_TRANSACTION);
    }

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
        mc.setTypeOfOngoingTransaction(XADiskManagedConnection.NO_TRANSACTION);
    }

    public Xid[] recover(int flag) throws XAException {
        return xaFileSystem.recover(flag);
    }

    public void forget(Xid xid) throws XAException {
        Xid internalXid = mapToInternalXid(xid);
    }

    public boolean isSameRM(XAResource xar) throws XAException {
        if (xar instanceof XAResourceImpl) {
            XAResourceImpl that = (XAResourceImpl) xar;
            if (this.mc instanceof XADiskRemoteManagedConnection) {
                if (that.mc instanceof XADiskRemoteManagedConnection) {
                    XADiskRemoteManagedConnection thisRMC = (XADiskRemoteManagedConnection) this.mc;
                    XADiskRemoteManagedConnection thatRMC = (XADiskRemoteManagedConnection) that.mc;
                    return thisRMC.pointsToSameRemoteXADisk(thatRMC);
                } else {
                    return false;
                }
            }
            //here, means this.mc is pointing to native xadisk.
            if (that.mc instanceof XADiskManagedConnection) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public int getTransactionTimeout() throws XAException {
        return transactionTimeout;
    }

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

/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.connector;

import org.xadisk.bridge.proxies.interfaces.Session;
import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.filesystem.NativeXASession;
import org.xadisk.filesystem.SessionCommonness;
import org.xadisk.filesystem.XAFileSystemCommonness;
import org.xadisk.filesystem.XidImpl;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;
import org.xadisk.filesystem.exceptions.XASystemException;

public class XAResourceImpl implements XAResource {

    private final NativeXASession xaSession;
    private final XAFileSystemCommonness xaFileSystem;
    private final ConcurrentHashMap<Xid, XidImpl> internalXids = new ConcurrentHashMap<Xid, XidImpl>(1000);
    private volatile int transactionTimeout;

    public XAResourceImpl(NativeXASession xaSession) {
        this.xaSession = xaSession;
        this.xaFileSystem = (XAFileSystemCommonness) xaSession.getUnderlyingXAFileSystem();
        transactionTimeout = xaFileSystem.getDefaultTransactionTimeout();
    }

    public void start(Xid xid, int flag) throws XAException {
        XidImpl internalXid = mapToInternalXid(xid);
        if (flag == XAResource.TMNOFLAGS) {
            try {
                Session session = xaSession.refreshSessionForNewXATransaction(internalXid);
                session.setTransactionTimeout(transactionTimeout);
            } catch (XASystemException xase) {
                throw new XAException(XAException.XAER_RMFAIL);
            }
            xaSession.setTypeOfCurrentTransaction(NativeXASession.XA_TRANSACTION);
        }
        if (flag == XAResource.TMJOIN) {
            Session sessionOfTransaction = xaFileSystem.getSessionForTransaction(internalXid);
            if (sessionOfTransaction == null) {
                throw new XAException(XAException.XAER_INVAL);
            }
            xaSession.setSessionOfExistingXATransaction(sessionOfTransaction);
            xaSession.setTypeOfCurrentTransaction(NativeXASession.XA_TRANSACTION);
        }
        if (flag == XAResource.TMRESUME) {
            Session sessionOfTransaction = xaFileSystem.getSessionForTransaction(internalXid);
            if (sessionOfTransaction == null) {
                throw new XAException(XAException.XAER_INVAL);
            }
            xaSession.setSessionOfExistingXATransaction(sessionOfTransaction);
            xaSession.setTypeOfCurrentTransaction(NativeXASession.XA_TRANSACTION);
        }
    }

    public void end(Xid xid, int flag) throws XAException {
        XidImpl internalXid = mapToInternalXid(xid);
        Session sessionOfTransaction = xaFileSystem.getSessionForTransaction(internalXid);
        if (sessionOfTransaction == null) {
            throw new XAException(XAException.XAER_INVAL);
        }
        if (flag == XAResource.TMSUCCESS) {
            xaSession.setTypeOfCurrentTransaction(NativeXASession.NO_TRANSACTION);
        }
        if (flag == XAResource.TMFAIL) {
            xaSession.setTypeOfCurrentTransaction(NativeXASession.NO_TRANSACTION);
        }
        if (flag == XAResource.TMSUSPEND) {
            xaSession.setTypeOfCurrentTransaction(NativeXASession.NO_TRANSACTION);
        }
    }

    public int prepare(Xid xid) throws XAException {
        XidImpl internalXid = mapToInternalXid(xid);
        Session sessionOfTransaction = xaFileSystem.getSessionForTransaction(internalXid);
        if (sessionOfTransaction == null) {
            throw new XAException(XAException.XAER_INVAL);
        }
        try {
            if(((SessionCommonness)sessionOfTransaction).isUsingReadOnlyOptimization()) {
                ((SessionCommonness)sessionOfTransaction).completeReadOnlyTransaction();
                return XAResource.XA_RDONLY;
            } else {
                ((SessionCommonness)sessionOfTransaction).prepare();
                return XAResource.XA_OK;
            }
        } catch (NoTransactionAssociatedException note) {
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
        } catch (NoTransactionAssociatedException note) {
            throw new XAException(XAException.XA_RBROLLBACK);
        } catch (XASystemException xase) {
            throw new XAException(XAException.XAER_RMFAIL);
        } finally {
            releaseFromInternalXidMap(xid);
        }
        xaSession.setTypeOfCurrentTransaction(NativeXASession.NO_TRANSACTION);
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        XidImpl internalXid = mapToInternalXid(xid);
        Session sessionOfTransaction = xaFileSystem.getSessionForTransaction(internalXid);
        if (sessionOfTransaction == null) {
            throw new XAException(XAException.XAER_INVAL);
        }
        try {
            ((SessionCommonness)sessionOfTransaction).commit(onePhase);
        } catch (NoTransactionAssociatedException note) {
            throw new XAException(XAException.XA_RBROLLBACK);
        } catch (XASystemException xase) {
            throw new XAException(XAException.XAER_RMFAIL);
        } finally {
            releaseFromInternalXidMap(xid);
        }
        xaSession.setTypeOfCurrentTransaction(NativeXASession.NO_TRANSACTION);
    }

    public Xid[] recover(int flag) throws XAException {
        return xaFileSystem.recover(flag);
    }

    public void forget(Xid xid) throws XAException {
        //Xid internalXid = mapToInternalXid(xid);
    }

    public boolean isSameRM(XAResource xar) throws XAException {
        if (xar instanceof XAResourceImpl) {
            XAResourceImpl that = (XAResourceImpl) xar;
            return this.xaFileSystem.pointToSameXAFileSystem(that.xaFileSystem);
        } else {
            return false;
        }
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

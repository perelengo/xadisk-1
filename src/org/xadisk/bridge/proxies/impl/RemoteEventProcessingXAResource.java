package org.xadisk.bridge.proxies.impl;

import java.io.IOException;
import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.filesystem.XidImpl;

public class RemoteEventProcessingXAResource extends RemoteObjectProxy implements XAResource {

    private transient ConcurrentHashMap<Xid, XidImpl> internalXids = new ConcurrentHashMap<Xid, XidImpl>();

    private void readObject(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.internalXids = new ConcurrentHashMap<Xid, XidImpl>();
    }

    public RemoteEventProcessingXAResource(long objectId, RemoteMethodInvoker invoker) {
        super(objectId, invoker);
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        try {
            XidImpl xidImpl = mapToInternalXid(xid);
            invokeRemoteMethod("commit", xidImpl, onePhase);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        } finally {
            this.shutdown();
        }
    }

    public void end(Xid xid, int flags) throws XAException {
        try {
            XidImpl xidImpl = mapToInternalXid(xid);
            invokeRemoteMethod("end", xidImpl, flags);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void forget(Xid xid) throws XAException {
        try {
            XidImpl xidImpl = mapToInternalXid(xid);
            invokeRemoteMethod("forget", xidImpl);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public int getTransactionTimeout() throws XAException {
        try {
            return (Integer) invokeRemoteMethod("getTransactionTimeout");
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public boolean isSameRM(XAResource xar) throws XAException {
        //it was dangerous (wrong) in the earlier impl to compare the invoker and say "same" if
        //the remoteAddr/Port was same. Note that the remote object, mapped using objectId,
        //might be release anytime, so we should be careful in so that these pointers (IDs)
        //to be referenced. For example, we have would be removing the remote XAR after say commit,
        //the object ID of the "sameRM"'s XAR would be pointing to a non-existing object.
        //Now, for this example, this was not the case as we didn't removed the remote LocalXAR.
        //But the main problem was that the objects of LocalEventProcessingXAResource are
        //use-once only as is visible from their field variables.
        return false;
    }

    public int prepare(Xid xid) throws XAException {
        try {
            XidImpl xidImpl = mapToInternalXid(xid);
            return (Integer) invokeRemoteMethod("prepare", xidImpl);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public Xid[] recover(int flag) throws XAException {
        try {
            return (Xid[]) invokeRemoteMethod("recover", flag);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void rollback(Xid xid) throws XAException {
        try {
            XidImpl xidImpl = mapToInternalXid(xid);
            invokeRemoteMethod("rollback", xidImpl);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        } finally {
            //an assumption that this XAR would be used only for a single txn in lifetime. Though its not always true.
            //But correctness is anyway retained, because after disconnection, it would automatically re-connect in case of further method invocations.
            this.shutdown();
        }
    }

    public boolean setTransactionTimeout(int timeout) throws XAException {
        try {
            return (Boolean) invokeRemoteMethod("setTransactionTimeout", timeout);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void start(Xid xid, int flag) throws XAException {
        try {
            XidImpl xidImpl = mapToInternalXid(xid);
            invokeRemoteMethod("start", xidImpl, flag);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
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

    private void shutdown() {
        try {
            this.invoker.disconnect();
        } catch (IOException ioe) {
            //no-op.
        }
    }
}

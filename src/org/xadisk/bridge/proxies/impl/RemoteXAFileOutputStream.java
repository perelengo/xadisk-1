package org.xadisk.bridge.proxies.impl;

import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.filesystem.exceptions.ClosedStreamException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;

public class RemoteXAFileOutputStream extends RemoteObjectProxy implements XAFileOutputStream {

    private static final long serialVersionUID = 1L;
    
    public RemoteXAFileOutputStream(long objectId, RemoteMethodInvoker invoker) {
        super(objectId, invoker);
    }

    public void write(int b) throws ClosedStreamException, TransactionRolledbackException {
        try {
            invokeRemoteMethod("write", b);
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void write(byte[] b) throws ClosedStreamException, TransactionRolledbackException {
        try {
            invokeRemoteMethod("write", b);
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void write(byte[] b, int off, int len) throws ClosedStreamException, TransactionRolledbackException {
        try {
            byte[] onWire = new byte[len];
            System.arraycopy(b, off, onWire, 0, len);
            invokeRemoteMethod("write", b, off, len);
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void flush() throws ClosedStreamException, TransactionRolledbackException {
        try {
            invokeRemoteMethod("flush");
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void close() throws TransactionRolledbackException {
        try {
            invokeRemoteMethod("close");
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public boolean isClosed() {
        try {
            return (Boolean) invokeRemoteMethod("isClosed");
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }
}

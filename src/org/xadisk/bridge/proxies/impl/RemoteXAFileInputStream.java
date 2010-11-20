package org.xadisk.bridge.proxies.impl;

import org.xadisk.bridge.proxies.facilitators.ByteArrayRemoteReference;
import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.filesystem.exceptions.ClosedStreamException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;

public class RemoteXAFileInputStream extends RemoteObjectProxy implements XAFileInputStream {

    public RemoteXAFileInputStream(long objectId, RemoteMethodInvoker invoker) {
        super(objectId, invoker);
    }

    public int available() throws TransactionRolledbackException, ClosedStreamException {
        try {
            return (Integer) invokeRemoteMethod("available");
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

    public void position(long n) throws TransactionRolledbackException, ClosedStreamException {
        try {
            invokeRemoteMethod("position", n);
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public long position() {
        try {
            return (Long) invokeRemoteMethod("position");
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public int read() throws ClosedStreamException, TransactionRolledbackException {
        try {
            return (Integer) invokeRemoteMethod("read");
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public int read(byte[] b) throws ClosedStreamException, TransactionRolledbackException {
        try {
            ByteArrayRemoteReference ref = new ByteArrayRemoteReference(b, 0, b.length);
            return (Integer) invokeRemoteMethod("read", ref);
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public int read(byte[] b, int off, int len) throws ClosedStreamException, TransactionRolledbackException {
        try {
            ByteArrayRemoteReference ref = new ByteArrayRemoteReference(b, off, len);
            return (Integer) invokeRemoteMethod("read", ref);
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public long skip(long n) throws TransactionRolledbackException, ClosedStreamException {
        try {
            return (Long) invokeRemoteMethod("skip", n);
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (ClosedStreamException cse) {
            throw cse;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }
}

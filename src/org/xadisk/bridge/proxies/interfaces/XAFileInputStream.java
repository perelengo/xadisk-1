package org.xadisk.bridge.proxies.interfaces;

import org.xadisk.filesystem.exceptions.ClosedStreamException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;

public interface XAFileInputStream {

    public int available() throws TransactionRolledbackException, ClosedStreamException;

    public void close() throws TransactionRolledbackException;

    public int read() throws ClosedStreamException, TransactionRolledbackException;

    public int read(byte[] b) throws ClosedStreamException, TransactionRolledbackException;

    public int read(byte[] b, int off, int len) throws ClosedStreamException, TransactionRolledbackException;

    public long skip(long n) throws TransactionRolledbackException, ClosedStreamException;

    public void position(long n) throws TransactionRolledbackException, ClosedStreamException;

    public boolean isClosed();
}

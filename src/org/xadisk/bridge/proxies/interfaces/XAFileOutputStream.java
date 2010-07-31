package org.xadisk.bridge.proxies.interfaces;

import org.xadisk.filesystem.exceptions.ClosedStreamException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;

public interface XAFileOutputStream {
    
    public void write(byte[] b) throws ClosedStreamException, TransactionRolledbackException;

    public void write(int b) throws ClosedStreamException, TransactionRolledbackException;

    public void write(byte[] b, int off, int len) throws ClosedStreamException, TransactionRolledbackException;

    public void flush() throws ClosedStreamException, TransactionRolledbackException;

    public void close() throws TransactionRolledbackException;
    
    public boolean isClosed();
}

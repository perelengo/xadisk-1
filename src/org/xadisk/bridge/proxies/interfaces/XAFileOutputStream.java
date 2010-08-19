package org.xadisk.bridge.proxies.interfaces;

import org.xadisk.filesystem.exceptions.ClosedStreamException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;

/**
 * Represents an output stream to a file.
 */
public interface XAFileOutputStream {

    /**
     * Writes all bytes from "b" into the file.
     * @param b the byte array.
     * @throws ClosedStreamException
     * @throws TransactionRolledbackException
     */
    public void write(byte[] b) throws ClosedStreamException, TransactionRolledbackException;

    /**
     * Writes the given byte into the file.
     * @param b
     * @throws ClosedStreamException
     * @throws TransactionRolledbackException
     */
    public void write(int b) throws ClosedStreamException, TransactionRolledbackException;

    /**
     * Writes bytes from array "b", starting at offset "off", upto total "length" bytes,
     * into the file.
     * @param b the byte array.
     * @param off offset in the byte array.
     * @param length length of the data to write.
     * @throws ClosedStreamException
     * @throws TransactionRolledbackException
     */
    public void write(byte[] b, int off, int length) throws ClosedStreamException, TransactionRolledbackException;

    /**
     * Flushes the buffer of this stream. This may not imply that the data gets written
     * to the disk.
     * @throws ClosedStreamException
     * @throws TransactionRolledbackException
     */
    public void flush() throws ClosedStreamException, TransactionRolledbackException;

    /**
     * Closes this stream. After closing, this stream becomes invalid for any i/o operations.
     * @throws TransactionRolledbackException
     */
    public void close() throws TransactionRolledbackException;

    /**
     * Tells whether this stream has been closed.
     * @return
     */
    public boolean isClosed();
}

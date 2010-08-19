package org.xadisk.bridge.proxies.interfaces;

import org.xadisk.filesystem.exceptions.ClosedStreamException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;

/**
 * Represents an input stream to a file.
 */
public interface XAFileInputStream {

    /**
     * Returns the number of bytes available in the buffer of this stream.
     * @return
     * @throws TransactionRolledbackException
     * @throws ClosedStreamException
     */
    public int available() throws TransactionRolledbackException, ClosedStreamException;

    /**
     * Closes this stream. After closing, this stream becomes invalid for any i/o operations.
     * @throws TransactionRolledbackException
     */
    public void close() throws TransactionRolledbackException;

    /**
     * Reads the next byte if available. If EOF has been reached, returns -1.
     * @return
     * @throws ClosedStreamException
     * @throws TransactionRolledbackException
     */
    public int read() throws ClosedStreamException, TransactionRolledbackException;

    /**
     * Read at least 1 byte, upto b.length bytes from the stream and put them into the array "b".
     * @param b the byte array into which the bytes will be read.
     * @return number of bytes actually read. -1 if EOF has been reached before reading even 1 byte.
     * @throws ClosedStreamException
     * @throws TransactionRolledbackException
     */
    public int read(byte[] b) throws ClosedStreamException, TransactionRolledbackException;

    /**
     * Read at least 1 byte, upto "length" bytes from the stream and put them into the array "b"
     * starting at offset "off".
     * @param b the byte array into which the bytes will be read.
     * @param off offset in the byte array.
     * @param length maximum number of bytes to read
     * @return number of bytes actually read. -1 if EOF has been reached before reading even 1 byte.
     * @throws ClosedStreamException
     * @throws TransactionRolledbackException
     */
    public int read(byte[] b, int off, int length) throws ClosedStreamException, TransactionRolledbackException;

    /**
     * Skips upto "n" bytes in the stream.
     * @param n Number of bytes to skip.
     * @return Actual number of bytes that were skipped.
     * @throws TransactionRolledbackException
     * @throws ClosedStreamException
     */
    public long skip(long n) throws TransactionRolledbackException, ClosedStreamException;

    /**
     * Positions the "pointer" inside the stream from where the next bytes will be read.
     * @param n The new position; should be within 0 and fileLength, both inclusive.
     * @throws TransactionRolledbackException
     * @throws ClosedStreamException
     */
    public void position(long n) throws TransactionRolledbackException, ClosedStreamException;

    /**
     * Tells whether this stream has been closed.
     * @return
     */
    public boolean isClosed();
}

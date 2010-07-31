package org.xadisk.filesystem.virtual;

import org.xadisk.filesystem.pools.PooledBuffer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.ReentrantLock;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.filesystem.Buffer;
import org.xadisk.filesystem.NativeSession;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.exceptions.ClosedStreamException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;

public class NativeXAFileInputStream implements XAFileInputStream {

    private FileChannel physicalFileChannel;
    private ByteBuffer byteBuffer;
    private final NativeXAFileSystem theXAFileSystem;
    private boolean filledAtleastOnce = false;
    private final VirtualViewFile vvf;
    private boolean closed = false;
    private long position;
    private final ByteBuffer cachedWritableByteBuffer;
    private int headerLengthInByteBuffer;
    private final NativeSession owningSession;
    private final ReentrantLock asynchronousRollbackLock;
    private final PooledBuffer pooledBuffer;

    public NativeXAFileInputStream(VirtualViewFile vvf, NativeSession owningSession) throws FileNotExistsException {
        this.theXAFileSystem = NativeXAFileSystem.getXAFileSystem();
        pooledBuffer = this.theXAFileSystem.getBufferPool().checkOut();
        if (pooledBuffer != null) {
            this.byteBuffer = pooledBuffer.getBuffer();
        } else {
            this.byteBuffer = (new Buffer(theXAFileSystem.getConfiguredBufferSize(), false)).getBuffer();
        }
        this.cachedWritableByteBuffer = this.byteBuffer;
        assert cachedWritableByteBuffer != null;//to debug a strange issue where refillBuffer was reporting
        //null for "cachedWritableBB".
        assert !cachedWritableByteBuffer.isReadOnly();//to debug another issue where this was reported to be r/o.
        this.vvf = vvf;
        vvf.addBeingRead();
        this.position = 0;
        this.owningSession = owningSession;
        this.asynchronousRollbackLock = owningSession.getAsynchronousRollbackLock();
        if (vvf.isMappedToAPhysicalFile()) {
            try {
                this.physicalFileChannel = new FileInputStream(vvf.getMappedToPhysicalFile()).getChannel();
            } catch (FileNotFoundException fnfe) {
                throw new FileNotExistsException();
            }
        }
    }

    public int available() throws TransactionRolledbackException, ClosedStreamException {
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            if (!filledAtleastOnce) {
                return 0;
            }
            return byteBuffer.remaining();
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    public void close() throws TransactionRolledbackException {
        if (closed) {
            return;
        }
        try {
            asynchronousRollbackLock.lock();
            owningSession.checkIfCanContinue();
            if (physicalFileChannel != null) {
                try {
                    physicalFileChannel.close();
                } catch (IOException ioe) {
                    theXAFileSystem.notifySystemFailure(ioe);
                }
            }
            vvf.reduceBeingRead();
            theXAFileSystem.getBufferPool().checkIn(pooledBuffer);
            closed = true;
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    public int read() throws ClosedStreamException, TransactionRolledbackException {
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            if (!filledAtleastOnce) {
                refillBuffer();
            }
            int nextByte;
            try {
                nextByte = byteBuffer.get();
            } catch (BufferUnderflowException bue) {
                int numRead = refillBuffer();
                if (numRead == -1) {
                    return -1;
                }
                nextByte = byteBuffer.get();
            }
            return nextByte;
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    public int read(byte[] b) throws ClosedStreamException, TransactionRolledbackException {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws ClosedStreamException, TransactionRolledbackException {
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            if (!filledAtleastOnce) {
                refillBuffer();
            }
            int remaining = byteBuffer.remaining();
            if (remaining == 0) {
                int numRead = refillBuffer();
                if (numRead == -1) {
                    return -1;
                }
            }
            remaining = byteBuffer.remaining();
            if (remaining < len) {
                len = remaining;
            }
            byteBuffer.get(b, off, len);
            return len;
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    public long skip(long n) throws TransactionRolledbackException, ClosedStreamException {
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            if (n < 0) {
                throw new IllegalArgumentException("Argument should be a non-negative integer.");
            }
            long filesize = vvf.getLength();
            long readPositionAfterSkip = (position - byteBuffer.remaining()) + n;
            if (readPositionAfterSkip > filesize) {
                n = n - (readPositionAfterSkip - filesize);
                readPositionAfterSkip = filesize;
            }
            position(readPositionAfterSkip);
            return n;
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    public void position(long n) throws TransactionRolledbackException, ClosedStreamException {
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();

            if(!filledAtleastOnce) {
                position = n;
                return;
            }
            
            long filesize = vvf.getLength();
            if (n < 0 || n > filesize) {
                throw new IllegalArgumentException("New position cannot be negative or more than file size.");
            }
            long oldReadPosition = position - byteBuffer.remaining();
            boolean isAhead = n - oldReadPosition > 0;
            long amountOfMove = Math.abs(n - oldReadPosition);
            if (isAhead) {
                if (amountOfMove > byteBuffer.remaining()) {
                    position = n;
                    byteBuffer.position(byteBuffer.limit());
                } else {
                    byteBuffer.position(byteBuffer.position() + (int) amountOfMove);
                }
            } else {
                if (amountOfMove > byteBuffer.position() - this.headerLengthInByteBuffer) {
                    position = n;
                    byteBuffer.position(byteBuffer.limit());
                } else {
                    byteBuffer.position(byteBuffer.position() - (int) amountOfMove);
                }
            }
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    private int refillBuffer() {
        try {
            byteBuffer = cachedWritableByteBuffer;
            byteBuffer.clear();
            int numRead = 0;
            if (vvf.isUsingHeavyWriteOptimization()) {
                numRead = vvf.fillUpContentsFromChannel(byteBuffer, position);
                byteBuffer.flip();
                if (numRead != -1) {
                    filledAtleastOnce = true;
                    position += numRead;
                }
                this.headerLengthInByteBuffer = 0;
                return numRead;
            }
            if (position <= vvf.getMappedToThePhysicalFileTill() - 1) {
                long maxAmountToBeRead = vvf.getMappedToThePhysicalFileTill() - position;
                if (maxAmountToBeRead < byteBuffer.limit()) {
                    byteBuffer.limit((int) maxAmountToBeRead);
                }
                physicalFileChannel.position(position);
                while (numRead == 0) {
                    numRead = physicalFileChannel.read(byteBuffer);
                }
                if (numRead != -1) {
                    position += numRead;
                    filledAtleastOnce = true;
                }
                byteBuffer.flip();
            } else {
                Buffer newBuffer = vvf.getInMemoryContentBuffer(position);
                if (newBuffer == null) {
                    return -1;
                }
                newBuffer = newBuffer.createReadOnlyClone();
                if (newBuffer.getBuffer() == null) {
                    int offsetInNewBuffer = (int) (position - newBuffer.getFileContentPosition());
                    numRead = newBuffer.regenerateContentFromDisk(byteBuffer, offsetInNewBuffer);
                    if (numRead != -1) {
                        position += numRead;
                        filledAtleastOnce = true;
                    }
                } else {
                    this.byteBuffer = newBuffer.getBuffer();
                    int offsetInNewBuffer = (int) (position - newBuffer.getFileContentPosition());
                    this.byteBuffer.position(newBuffer.getHeaderLength() + offsetInNewBuffer);
                    this.byteBuffer.limit(newBuffer.getHeaderLength() + newBuffer.getFileContentLength());
                    position = position + newBuffer.getFileContentLength() - offsetInNewBuffer;
                    filledAtleastOnce = true;
                    numRead = 0;
                }
            }
            this.headerLengthInByteBuffer = this.byteBuffer.position();
            return numRead;
        } catch (IOException ioe) {
            theXAFileSystem.notifySystemFailure(ioe);
            return -1;
        }
    }

    public boolean isClosed() {
        return closed;
    }

    private void checkIfCanContinue() throws TransactionRolledbackException, ClosedStreamException {
        if (closed) {
            throw new ClosedStreamException();
        }
        owningSession.checkIfCanContinue();
    }

    public File getSourceFileName() {
        return vvf.getFileName();
    }
}

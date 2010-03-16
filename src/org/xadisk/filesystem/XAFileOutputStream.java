package org.xadisk.filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.xadisk.filesystem.exceptions.ClosedStreamException;
import org.xadisk.filesystem.exceptions.FileUnderUseException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;

public class XAFileOutputStream {

    private final String destination;
    private ByteBuffer byteBuffer;
    private Buffer buffer;
    private final XAFileSystem theXAFileSystem;
    private final XidImpl xid;
    private final GatheringDiskWriter theGatheringDiskWriter;
    private long filePosition;
    private boolean closed = false;
    private final VirtualViewFile vvf;
    private final boolean heavyWrite;
    private final Session owningSession;
    private final ReentrantLock asynchronousRollbackLock;
    private static final HashMap<File, XAFileOutputStream> fileAndOutputStream = new HashMap<File, XAFileOutputStream>(1000);

    XAFileOutputStream(VirtualViewFile vvf, XidImpl xid, boolean heavyWrite,
            Session owningSession) {
        this.theXAFileSystem = XAFileSystem.getXAFileSystem();
        this.destination = vvf.getFileName().getAbsolutePath();
        this.xid = xid;
        this.theGatheringDiskWriter = this.theXAFileSystem.getTheGatheringDiskWriter();
        this.vvf = vvf;
        this.filePosition = vvf.getLength();
        vvf.setBeingWritten(true);
        if (heavyWrite) {
            if (!vvf.isUsingHeavyWriteOptimization()) {
                try {
                    vvf.setUpForHeavyWriteOptimization();
                } catch (IOException ioe) {
                    theXAFileSystem.notifySystemFailure(ioe);
                }
            }
        }
        this.heavyWrite = vvf.isUsingHeavyWriteOptimization();
        allocateByteBuffer();
        setUpNewBuffer();
        this.owningSession = owningSession;
        this.asynchronousRollbackLock = owningSession.getAsynchronousRollbackLock();
    }

    public void write(byte[] b) throws ClosedStreamException, TransactionRolledbackException {
        write(b, 0, b.length);
    }

    public void write(int b) throws ClosedStreamException, TransactionRolledbackException {
        byte b1[] = {(byte) b};
        write(b1, 0, 1);
    }

    public void write(byte[] b, int off, int len) throws ClosedStreamException, TransactionRolledbackException {
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            int len1 = len;
            if (byteBuffer.remaining() < len) {
                len1 = byteBuffer.remaining();
            }
            byteBuffer.put(b, off, len1);
            filePosition += len1;
            if (byteBuffer.remaining() == 0) {
                submitBuffer();
                setUpNewBuffer();
            }
            if (len1 < len) {
                write(b, off + len1, len - len1);
            }
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    public void flush() throws ClosedStreamException, TransactionRolledbackException {
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            submitBuffer();
            setUpNewBuffer();
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
            submitBuffer();
            vvf.setBeingWritten(false);
            closed = true;
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    private void allocateByteBuffer() {
        buffer = theXAFileSystem.getBufferPool().checkOut();
        if (buffer != null) {
            this.byteBuffer = buffer.getBuffer();
        } else {
            this.buffer = new Buffer(theXAFileSystem.getConfiguredBufferSize(), false);
            this.byteBuffer = buffer.getBuffer();
        }
        this.byteBuffer.clear();
    }

    private void setUpNewBuffer() {
        if (heavyWrite) {
            this.byteBuffer.clear();
        } else {
            allocateByteBuffer();
            byte temp[] = TransactionLogEntry.getLogEntry(xid, destination, filePosition, 21, TransactionLogEntry.FILE_APPEND);
            byteBuffer.put(temp);
            buffer.setFileContentPosition(filePosition);
            buffer.setHeaderLength(temp.length);
        }
    }

    private void submitBuffer() {
        try {
            if (heavyWrite) {
                byteBuffer.flip();
                vvf.appendContentBuffer(buffer);
            } else {
                int contentLength = byteBuffer.position() - buffer.getHeaderLength();
                TransactionLogEntry.updateContentLength(byteBuffer, contentLength);
                buffer.setFileContentLength(contentLength);
                byteBuffer.flip();
                vvf.appendContentBuffer(buffer);
                theGatheringDiskWriter.submitBuffer(buffer, xid);
            }
        } catch (IOException ioe) {
            theXAFileSystem.notifySystemFailure(ioe);
        }
    }

    static XAFileOutputStream getCachedXAFileOutputStream(VirtualViewFile vvf, XidImpl xid, boolean heavyWrite,
            Session owningSession)
            throws FileUnderUseException {
        synchronized (fileAndOutputStream) {
            File f = vvf.getFileName();
            XAFileOutputStream xaFOS = fileAndOutputStream.get(f);
            if (xaFOS == null || xaFOS.isClosed()) {
                xaFOS = new XAFileOutputStream(vvf, xid, heavyWrite, owningSession);
                fileAndOutputStream.put(f, xaFOS);
            } else {
                if (!vvf.isUsingHeavyWriteOptimization() && heavyWrite) {
                    throw new FileUnderUseException();
                }
            }
            return xaFOS;
        }
    }

    static void deCacheXAFileOutputStream(File f) {
        synchronized (fileAndOutputStream) {
            fileAndOutputStream.remove(f);
        }
    }

    File getDestinationFile() {
        return new File(destination);
    }

    private void checkIfCanContinue() throws TransactionRolledbackException, ClosedStreamException {
        if (closed) {
            throw new ClosedStreamException();
        }
        owningSession.checkIfCanContinue();
    }

    public boolean isClosed() {
        return closed;
    }
}

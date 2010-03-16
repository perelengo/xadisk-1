package org.xadisk.filesystem;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;

class Buffer {

    ByteBuffer buffer;
    private final boolean hasItsOwnBytes;
    final boolean isDirect;
    private volatile OnDiskInfo onDiskInfo = null;
    private long fileContentPosition;
    private int fileContentLength;
    private int headerLength;
    private final static AtomicLong totalNonPooledBufferSize = new AtomicLong(0);
    private FileChannel logFileChannel;
    private int logChannelIndex = -1;
    private final XAFileSystem xaFileSystem = XAFileSystem.getXAFileSystem();
    private volatile boolean memorySynchTrigger = true;

    Buffer(int bufferSize, boolean isDirect) {
        if (isDirect) {
            this.buffer = ByteBuffer.allocateDirect(bufferSize);
        } else {
            this.buffer = ByteBuffer.allocate(bufferSize);
        }
        this.hasItsOwnBytes = true;
        this.isDirect = isDirect;
        if (!(this instanceof PooledBuffer)) {
            changeTotalNonPooledBufferSize(bufferSize);
        }
    }

    Buffer(ByteBuffer buffer) {
        this.buffer = buffer;
        this.isDirect = false;
        this.hasItsOwnBytes = true;
        if (!(this instanceof PooledBuffer)) {
            changeTotalNonPooledBufferSize(buffer.capacity());
        }
    }

    Buffer() {
        this.hasItsOwnBytes = false;
        this.isDirect = false;
    }

    private static void changeTotalNonPooledBufferSize(int changeAmount) {
        totalNonPooledBufferSize.addAndGet(changeAmount);
    }

    void flushByteBufferChanges() {
        memorySynchTrigger = true;
    }

    void invalidateByteBufferFromCache() {
        boolean dummyRead = memorySynchTrigger;
    }

    ByteBuffer getBuffer() {
        return buffer;
    }

    boolean isDirect() {
        return isDirect;
    }

    static long getTotalNonPooledBufferSize() {
        return totalNonPooledBufferSize.get();
    }

    void makeOnDisk(OnDiskInfo onDiskInfo) {
        if (onDiskInfo == null) {
            return;
        }
        this.onDiskInfo = onDiskInfo;
        if (!(this instanceof PooledBuffer)) {
            if (hasItsOwnBytes) {
                changeTotalNonPooledBufferSize(-buffer.capacity());
            }
            buffer = null;
        }
    }

    void setOnDiskInfo(OnDiskInfo onDiskInfo) {
        this.onDiskInfo = onDiskInfo;
    }

    @Override
    protected void finalize() throws Throwable {
        if (!(this instanceof PooledBuffer)) {
            if (buffer == null) {
            } else {
                if (hasItsOwnBytes) {
                    changeTotalNonPooledBufferSize(-buffer.capacity());
                }
            }
        }
    }

    OnDiskInfo getOnDiskInfo() {
        return onDiskInfo;
    }

    int getFileContentLength() {
        return fileContentLength;
    }

    void setFileContentLength(int fileContentLength) {
        this.fileContentLength = fileContentLength;
    }

    long getFileContentPosition() {
        return fileContentPosition;
    }

    void setFileContentPosition(long fileContentPosition) {
        this.fileContentPosition = fileContentPosition;
    }

    int getHeaderLength() {
        return headerLength;
    }

    void setHeaderLength(int headerLength) {
        this.headerLength = headerLength;
    }

    Buffer createReadOnlyClone() {
        Buffer clone = new Buffer();
        ByteBuffer referenceToByteBuffer = this.buffer;
        if (referenceToByteBuffer == null) {
            clone.onDiskInfo = this.onDiskInfo;
        } else {
            clone.buffer = referenceToByteBuffer.asReadOnlyBuffer();
        }
        clone.setFileContentLength(fileContentLength);
        clone.setFileContentPosition(fileContentPosition);
        clone.setHeaderLength(headerLength);
        return clone;
    }

    int regenerateContentFromDisk(ByteBuffer target, int offsetToReadFrom) throws IOException {
        int logIndex = onDiskInfo.getLogIndex();
        if (logChannelIndex != logIndex) {
            if (logFileChannel != null) {
                logFileChannel.close();
            }
            FileInputStream logIS = new FileInputStream(xaFileSystem.getTransactionLogFileBaseName() + "_" + logIndex);
            logFileChannel = logIS.getChannel();
            logChannelIndex = logIndex;
        }
        logFileChannel.position(onDiskInfo.getLocation() + headerLength + offsetToReadFrom);
        target.limit(fileContentLength - offsetToReadFrom);
        int numRead = 0;
        while (numRead == 0) {
            numRead = logFileChannel.read(target);
        }
        target.flip();
        return numRead;
    }
}

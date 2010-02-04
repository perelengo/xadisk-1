package org.xadisk.filesystem;

import java.nio.ByteBuffer;

class PooledBuffer extends Buffer {
    
    private volatile long lastFreed = -1;

    PooledBuffer(int bufferSize, boolean isDirect) {
        super(bufferSize, isDirect);
    }

    void markFree() {
        buffer.clear();
        lastFreed = System.currentTimeMillis() / 1000;
    }

    @Override
    ByteBuffer getBuffer() {
        return buffer;
    }

    long getLastFreed() {
        return lastFreed;
    }
}

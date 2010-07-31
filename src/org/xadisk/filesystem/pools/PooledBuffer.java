package org.xadisk.filesystem.pools;

import java.nio.ByteBuffer;
import org.xadisk.filesystem.Buffer;

public class PooledBuffer extends Buffer implements PooledResource {

    private volatile long lastFreed = -1;

    PooledBuffer(int bufferSize, boolean isDirect) {
        super(bufferSize, isDirect);
    }

    public void markFree() {
        buffer.clear();
        lastFreed = System.currentTimeMillis() / 1000;
    }

    @Override
    public ByteBuffer getBuffer() {
        return buffer;
    }

    public long getLastFreed() {
        return lastFreed;
    }
}

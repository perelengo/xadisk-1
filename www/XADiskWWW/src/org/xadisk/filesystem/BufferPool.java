package org.xadisk.filesystem;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

class BufferPool {

    private final int directBufferMaxPoolSize;
    private final int nonDirectBufferMaxPoolSize;
    private final int bufferSize;
    private final AtomicInteger currentDirectPoolSize;
    private final AtomicInteger currentNonDirectPoolSize;
    private final ConcurrentLinkedQueue<PooledBuffer> directFreeBuffers;
    private final ConcurrentLinkedQueue<PooledBuffer> nonDirectFreeBuffers;
    private final int directBufferIdleTime;
    private final int nonDirectBufferIdleTime;

    BufferPool(int directBufferPoolSize, int nonDirectBufferPoolSize, int bufferSize,
            int directBufferIdleTime, int nonDirectBufferIdleTime, XAFileSystem xaFileSystem) {
        this.directBufferMaxPoolSize = directBufferPoolSize;
        this.nonDirectBufferMaxPoolSize = nonDirectBufferPoolSize;
        this.bufferSize = bufferSize;
        this.directBufferIdleTime = directBufferIdleTime;
        this.nonDirectBufferIdleTime = nonDirectBufferIdleTime;
        this.directFreeBuffers = new ConcurrentLinkedQueue<PooledBuffer>();
        this.nonDirectFreeBuffers = new ConcurrentLinkedQueue<PooledBuffer>();
        this.currentDirectPoolSize = new AtomicInteger(0);
        this.currentNonDirectPoolSize = new AtomicInteger(0);
    }

    PooledBuffer checkOut() {
        PooledBuffer temp = lookIntoCurrentPool(true);
        if (temp != null) {
            return temp;
        }
        temp = lookIntoCurrentPool(false);
        if (temp != null) {
            return temp;
        }
        temp = allocateNewInCurrentPool(true);
        if (temp != null) {
            return temp;
        }
        temp = allocateNewInCurrentPool(false);
        if (temp != null) {
            return null;
        }
        return null;
    }

    private PooledBuffer lookIntoCurrentPool(boolean inDirectBufferPool) {
        ConcurrentLinkedQueue<PooledBuffer> buffers;
        if (inDirectBufferPool) {
            buffers = directFreeBuffers;
        } else {
            buffers = nonDirectFreeBuffers;
        }
        PooledBuffer freeBuffer = buffers.poll();
        if (freeBuffer != null) {
            freeBuffer.invalidateByteBufferFromCache();
        }
        return freeBuffer;
    }

    private PooledBuffer allocateNewInCurrentPool(boolean inDirectBufferPool) {
        AtomicInteger currentPoolSize;
        int maxPoolSize;
        PooledBuffer newBuffer = null;
        if (inDirectBufferPool) {
            currentPoolSize = currentDirectPoolSize;
            maxPoolSize = directBufferMaxPoolSize;
        } else {
            currentPoolSize = currentNonDirectPoolSize;
            maxPoolSize = nonDirectBufferMaxPoolSize;
        }
        while (true) {
            int temp = currentPoolSize.get();
            if (temp >= maxPoolSize) {
                return null;
            }
            if (currentPoolSize.compareAndSet(temp, temp + 1)) {
                break;
            }
        }
        newBuffer = new PooledBuffer(bufferSize, inDirectBufferPool);
        return newBuffer;
    }

    void checkIn(PooledBuffer buffer) {
        buffer.markFree();
        if (buffer.isDirect) {
            directFreeBuffers.offer(buffer);
        } else {
            nonDirectFreeBuffers.offer(buffer);
        }
        buffer.flushByteBufferChanges();
    }

    void freeIdleMembers() {
        freeIdleMembers(true);
        freeIdleMembers(false);
    }

    private void freeIdleMembers(boolean inDirectBufferPool) {
        AtomicInteger currentPoolSize;
        ConcurrentLinkedQueue<PooledBuffer> buffers;
        int bufferIdleTime;

        if (inDirectBufferPool) {
            currentPoolSize = currentDirectPoolSize;
            bufferIdleTime = directBufferIdleTime;
            buffers = directFreeBuffers;
        } else {
            currentPoolSize = currentNonDirectPoolSize;
            bufferIdleTime = nonDirectBufferIdleTime;
            buffers = nonDirectFreeBuffers;
        }
        long now = System.currentTimeMillis() / 1000;
        while (true) {
            PooledBuffer buffer = buffers.peek();
            if (buffer == null) {
                break;
            }
            if (now - buffer.getLastFreed() > bufferIdleTime) {
                if (buffers.remove(buffer)) {
                    currentPoolSize.decrementAndGet();
                }
            } else {
                break;
            }
        }
    }
}

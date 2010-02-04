package org.xadisk.filesystem;

class BufferPoolReliever extends TimedWorker {

    private final BufferPool bufferPool;

    BufferPoolReliever(BufferPool bufferPool, int frequency) {
        super(frequency);
        this.bufferPool = bufferPool;
    }

    @Override
    void doWorkOnce() {
        try {
            bufferPool.freeIdleMembers();
        } catch (Throwable t) {
            XAFileSystem.getXAFileSystem().notifySystemFailure(t);
        }
    }
}

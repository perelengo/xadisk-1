package org.xadisk.filesystem.workers;

import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.pools.ResourcePool;

public class ObjectPoolReliever extends TimedWorker {

    private final ResourcePool objectPool;

    public ObjectPoolReliever(ResourcePool objectPool, int frequency) {
        super(frequency);
        this.objectPool = objectPool;
    }

    @Override
    void doWorkOnce() {
        try {
            objectPool.freeIdleMembers();
        } catch (Throwable t) {
            NativeXAFileSystem.getXAFileSystem().notifySystemFailure(t);
        }
    }
}

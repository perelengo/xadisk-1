package org.xadisk.filesystem.pools;

public interface PooledResource {

    public void markFree();

    public long getLastFreed();
}

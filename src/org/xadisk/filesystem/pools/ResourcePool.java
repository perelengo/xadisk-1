package org.xadisk.filesystem.pools;

import org.xadisk.filesystem.*;

public interface ResourcePool<R extends PooledResource> {

    public R checkOut();

    public void checkIn(R r);

    public void freeIdleMembers();
}

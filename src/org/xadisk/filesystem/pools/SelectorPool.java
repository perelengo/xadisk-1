package org.xadisk.filesystem.pools;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SelectorPool implements ResourcePool<PooledSelector> {

    private final AtomicInteger currentPoolSize;
    private final ConcurrentLinkedQueue<PooledSelector> freeSelectors;
    private final int idleTime;

    public SelectorPool(int idleTime) {
        this.idleTime = idleTime;
        this.currentPoolSize = new AtomicInteger(0);
        this.freeSelectors = new ConcurrentLinkedQueue<PooledSelector>();
    }

    public PooledSelector checkOut() {
        PooledSelector temp = lookIntoCurrentPool();
        if (temp != null) {
            return temp;
        }
        temp = allocateNewInCurrentPool();
        if (temp != null) {
            return temp;
        }
        return null;
    }

    private PooledSelector lookIntoCurrentPool() {
        PooledSelector freeSelector = freeSelectors.poll();
        return freeSelector;
    }

    private PooledSelector allocateNewInCurrentPool() {
        PooledSelector newSelector = null;
        while (true) {
            try {
                newSelector = new PooledSelector();
            } catch (IOException ioe) {
                //allocation failed...return null.
            }
            int temp = currentPoolSize.get();
            if (currentPoolSize.compareAndSet(temp, temp + 1)) {
                break;
            }
        }
        return newSelector;
    }

    public void checkIn(PooledSelector selector) {
        freeSelectors.offer(selector);
    }

    public void freeIdleMembers() {
        long now = System.currentTimeMillis() / 1000;
        while (true) {
            PooledSelector selector = freeSelectors.peek();
            if (selector == null) {
                break;
            }
            if (now - selector.getLastFreed() > idleTime) {
                if (freeSelectors.remove(selector)) {
                    currentPoolSize.decrementAndGet();
                }
            } else {
                break;
            }
        }
    }
}

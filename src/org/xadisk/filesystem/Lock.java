package org.xadisk.filesystem;

import java.io.File;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class Lock {

    private final File resource;
    private boolean exclusive = false;
    private boolean upgraded = false;
    private int numHolders = 0;
    private final HashSet<XidImpl> holders = new HashSet<XidImpl>(10);
    private final ReentrantLock synchLock = new ReentrantLock(false);
    private final Condition mayBeReadable = synchLock.newCondition();
    private final Condition mayBeWritable = synchLock.newCondition();

    Lock(boolean exclusive, File resource) {
        this.exclusive = exclusive;
        this.resource = resource;
    }

    int getNumHolders() {
        return numHolders;
    }

    boolean isExclusive() {
        return exclusive;
    }

    void addHolder(XidImpl xid) {
        numHolders++;
        holders.add(xid);
    }

    void removeHolder(XidImpl xid) {
        numHolders--;
        holders.remove(xid);
    }

    HashSet<XidImpl> getHolders() {
        assert synchLock.isHeldByCurrentThread();
        return holders;
    }

    boolean isAHolder(XidImpl xid) {
        return holders.contains(xid);
    }

    void reset() {
        exclusive = false;
        upgraded = false;
        numHolders = 0;
        holders.clear();
    }

    void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    File getResource() {
        return resource;
    }

    void markUpgraded() {
        this.upgraded = true;
    }

    boolean isUpgraded() {
        return upgraded;
    }

    void startSynchBlock() {
        synchLock.lock();
    }

    void endSynchBlock() {
        synchLock.unlock();
    }

    void waitTillReadable(long time) throws InterruptedException {
        mayBeReadable.await(time, TimeUnit.MILLISECONDS);
    }

    void waitTillWritable(long time) throws InterruptedException {
        mayBeWritable.await(time, TimeUnit.MILLISECONDS);
    }
    
    void notifyWritable() {
        mayBeWritable.signal();
    }

    void notifyReadWritable() {
        mayBeReadable.signalAll();
        mayBeWritable.signal();
    }
}

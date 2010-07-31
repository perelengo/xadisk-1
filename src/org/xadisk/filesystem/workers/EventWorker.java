package org.xadisk.filesystem.workers;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.resource.spi.work.Work;

public abstract class EventWorker implements Work {

    private final ReentrantLock eventRaiseSynchLock = new ReentrantLock(false);
    private final Condition waitTillEventRaised = eventRaiseSynchLock.newCondition();
    private boolean enabled = true;
    private boolean eventRaised = false;

    public void release() {
        try {
            eventRaiseSynchLock.lockInterruptibly();
            enabled = false;
            waitTillEventRaised.signal();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
        } finally {
            eventRaiseSynchLock.unlock();
        }
    }

    public void run() {
        while (enabled) {
            try {
                eventRaiseSynchLock.lockInterruptibly();
                while (!eventRaised && enabled) {
                    waitTillEventRaised.await();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                eventRaiseSynchLock.unlock();
            }
            if (eventRaised) {
                eventRaised = false;
                processEvent();
            }
        }
    }

    abstract void processEvent();

    void raiseEvent() {
        if (eventRaised) {
            return;
        }
        eventRaised = true;
        try {
            eventRaiseSynchLock.lock();
            waitTillEventRaised.signal();
        } finally {
            eventRaiseSynchLock.unlock();
        }
    }
}

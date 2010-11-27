package org.xadisk.filesystem.workers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.resource.spi.work.Work;

public abstract class TimedWorker implements Work {

    private final int frequency;
    private final ReentrantLock wakeUpAndDieAlarm = new ReentrantLock(false);
    private final Condition hasBeenReleased = wakeUpAndDieAlarm.newCondition();
    private boolean released = false;

    TimedWorker(int frequency) {
        this.frequency = frequency;
    }

    public void release() {
        try {
            wakeUpAndDieAlarm.lockInterruptibly();
            released = true;
            hasBeenReleased.signal();
        } catch(InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
        }finally {
            wakeUpAndDieAlarm.unlock();
        }
    }

    public void run() {
        while (!released) {
            doWorkOnce();
            try {
                wakeUpAndDieAlarm.lockInterruptibly();
                hasBeenReleased.await(frequency * 1000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                wakeUpAndDieAlarm.unlock();
            }
        }
    }

    abstract void doWorkOnce();

    int getFrequency() {
        return frequency;
    }
}

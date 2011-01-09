package org.xadisk.tests.performance;

import java.util.concurrent.atomic.AtomicLong;

public abstract class TimeMeasuredWork implements Runnable {

    private AtomicLong timeTaken;
    private boolean useXADisk;

    public TimeMeasuredWork(AtomicLong timeTaken, boolean useXADisk) {
        this.timeTaken = timeTaken;
        this.useXADisk = useXADisk;
    }

    public void run() {
        try {
            Thread.sleep(1000);//to allow other threads to "start".
            long startTime = System.currentTimeMillis();
            if(useXADisk) {
                doWorkViaXADisk();
            } else {
                doWorkDirectly();
            }
            long endTime = System.currentTimeMillis();
            timeTaken.addAndGet(endTime - startTime);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract void doWorkDirectly() throws Exception;

    protected abstract void doWorkViaXADisk() throws Exception;
}

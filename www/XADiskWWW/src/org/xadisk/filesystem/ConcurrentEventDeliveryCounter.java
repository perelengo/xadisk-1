package org.xadisk.filesystem;

import java.util.concurrent.atomic.AtomicInteger;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

class ConcurrentEventDeliveryCounter implements WorkListener {

    private final AtomicInteger ongoingConcurrentDeliveries=new AtomicInteger(0);

    @Override
    public void workCompleted(WorkEvent we) {
        ongoingConcurrentDeliveries.decrementAndGet();
    }

    @Override
    public void workStarted(WorkEvent we) {
        ongoingConcurrentDeliveries.incrementAndGet();
    }

    int getOngoingConcurrentDeliveries() {
        return ongoingConcurrentDeliveries.get();
    }

    @Override
    public void workAccepted(WorkEvent we) {
    }

    @Override
    public void workRejected(WorkEvent we) {
    }
}

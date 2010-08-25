package org.xadisk.filesystem.workers.observers;

import java.util.concurrent.atomic.AtomicInteger;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

public class EventDispatchListener implements WorkListener {

    private final AtomicInteger ongoingConcurrentDeliveries=new AtomicInteger(0);

    public void workCompleted(WorkEvent we) {
        ongoingConcurrentDeliveries.decrementAndGet();
    }

    public void workStarted(WorkEvent we) {
        ongoingConcurrentDeliveries.incrementAndGet();
    }

    public int getOngoingConcurrentDeliveries() {
        return ongoingConcurrentDeliveries.get();
    }

    public void workAccepted(WorkEvent we) {
    }

    public void workRejected(WorkEvent we) {
    }
}

package org.xadisk.filesystem.workers;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;
import org.xadisk.bridge.proxies.impl.RemoteMessageEndpointFactory;
import org.xadisk.connector.inbound.EndPointActivation;
import org.xadisk.filesystem.FileStateChangeEvent;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.workers.observers.ConcurrentEventDeliveryCounter;

public class FileSystemEventDelegator implements Work {

    private final NativeXAFileSystem xaFileSystem;
    private final LinkedBlockingQueue<FileStateChangeEvent> eventQueue;
    private final WorkManager workManager;
    private final CopyOnWriteArrayList<EndPointActivation> registeredActivations =
            new CopyOnWriteArrayList<EndPointActivation>();
    private final int maximumConcurrentEventDeliveries;
    private final ConcurrentEventDeliveryCounter concurrentEventDeliveryCounter;
    private volatile boolean released = false;

    public FileSystemEventDelegator(NativeXAFileSystem xaFileSystem, int maximumConcurrentEventDeliveries) {
        this.xaFileSystem = xaFileSystem;
        this.eventQueue = xaFileSystem.getFileSystemEventQueue();
        this.workManager = NativeXAFileSystem.getWorkManager();
        this.maximumConcurrentEventDeliveries = maximumConcurrentEventDeliveries;
        this.concurrentEventDeliveryCounter = new ConcurrentEventDeliveryCounter();
    }

    public void registerActivation(EndPointActivation activation) {
        registeredActivations.add(activation);
    }

    public void deRegisterActivation(EndPointActivation activation) {
        MessageEndpointFactory mef = activation.getMessageEndpointFactory();
        if (mef instanceof RemoteMessageEndpointFactory) {
            RemoteMessageEndpointFactory remoteMEF = (RemoteMessageEndpointFactory) mef;
            int registeredAt = registeredActivations.indexOf(activation);
            if (registeredAt != -1) {
                remoteMEF = (RemoteMessageEndpointFactory) registeredActivations.get(registeredAt).getMessageEndpointFactory();
                remoteMEF.shutdown();
            }
        }
        registeredActivations.remove(activation);
    }

    public void run() {
        try {
            while (!released) {
                if (concurrentEventDeliveryCounter.getOngoingConcurrentDeliveries()
                        >= maximumConcurrentEventDeliveries) {
                    Thread.sleep(100);
                    continue;
                }
                FileStateChangeEvent event = eventQueue.poll(1000, TimeUnit.MILLISECONDS);
                if (event == null) {
                    continue;
                }
                Iterator<EndPointActivation> activations = registeredActivations.iterator();
                EndPointActivation interestedActivationPicked = null;
                while (activations.hasNext()) {
                    EndPointActivation current = activations.next();
                    if (current.getActivationSpecImpl().isEndpointInterestedIn(event)) {
                        interestedActivationPicked = current;
                        break;
                    }
                }
                try {
                    if (interestedActivationPicked != null) {
                        workManager.startWork(new FileSystemEventProcessor(interestedActivationPicked.getMessageEndpointFactory(),
                                event, xaFileSystem), 0, null, concurrentEventDeliveryCounter);
                        concurrentEventDeliveryCounter.workStarted(null);
                    }
                } catch (WorkException we) {
                    eventQueue.put(event);//but this will disrupt the order of events in the queue. Any other
                    //way? Rejection? Dead-letter file where we can put this info?
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
        } catch (Throwable t) {
            xaFileSystem.notifySystemFailure(t);
        }
    }

    public void release() {
        released = true;
    }
}

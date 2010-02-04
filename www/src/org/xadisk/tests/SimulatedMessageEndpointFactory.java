package org.xadisk.tests;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import org.xadisk.connector.EventProcessingXAResource;
import org.xadisk.connector.FileSystemEventListener;
import org.xadisk.filesystem.FileStateChangeEvent;
import org.xadisk.filesystem.XAFileSystem;
import org.xadisk.filesystem.XidImpl;

public class SimulatedMessageEndpointFactory implements MessageEndpointFactory {

    private AtomicInteger eventsReceived = new AtomicInteger(0);

    @Override
    public MessageEndpoint createEndpoint(XAResource xar) throws UnavailableException {
        return new SimulatedMessageEndpoint((EventProcessingXAResource) xar, this);
    }

    @Override
    public boolean isDeliveryTransacted(Method meth) throws NoSuchMethodException {
        return true;
    }

    public void incrementEventsReceivedCount() {
        eventsReceived.getAndIncrement();
    }

    public int getEventsReceivedCount() {
        return eventsReceived.get();
    }
}

class SimulatedMessageEndpoint implements MessageEndpoint, FileSystemEventListener {

    private XAResource epXAR;
    private SimulatedMessageEndpointFactory owningFactory;
    private XidImpl generatedXid;

    public SimulatedMessageEndpoint(EventProcessingXAResource epXAR, SimulatedMessageEndpointFactory owningFactory) {
        this.epXAR = epXAR;
        this.owningFactory = owningFactory;
    }

    @Override
    public void beforeDelivery(Method meth) throws NoSuchMethodException, ResourceException {
        this.generatedXid = XidImpl.getXidInstanceForLocalTransaction(
                XAFileSystem.getXAFileSystem().getNextLocalTransactionId());
        try {
            epXAR.start(generatedXid, XAResource.TMNOFLAGS);
        } catch (XAException xae) {
            throw new ResourceException(xae);
        }
    }

    @Override
    public void onFileSystemEvent(FileStateChangeEvent event) {
        owningFactory.incrementEventsReceivedCount();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
    }

    @Override
    public void afterDelivery() throws ResourceException {
        try {
            epXAR.end(generatedXid, XAResource.TMSUCCESS);
            epXAR.prepare(generatedXid);
            epXAR.commit(generatedXid, false);
        } catch (XAException xae) {
            throw new ResourceException(xae);
        }
    }

    @Override
    public void release() {
    }
}

package org.xadisk.tests;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

public class SimulatedMessageEndpointFactory implements MessageEndpointFactory {

    private AtomicInteger eventsReceived = new AtomicInteger(0);

    public MessageEndpoint createEndpoint(XAResource xar) throws UnavailableException {
        return new SimulatedMessageEndpoint(xar, this);
    }

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
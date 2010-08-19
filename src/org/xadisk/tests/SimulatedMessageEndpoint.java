package org.xadisk.tests;

import java.lang.reflect.Method;
import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import org.xadisk.connector.inbound.FileSystemEventListener;
import org.xadisk.filesystem.FileStateChangeEvent;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.XidImpl;

public class SimulatedMessageEndpoint implements MessageEndpoint, FileSystemEventListener {

    private XAResource epXAR;
    private SimulatedMessageEndpointFactory owningFactory;
    private XidImpl generatedXid;
    public SimulatedMessageEndpointFactory.GoTill goTill;

    public SimulatedMessageEndpoint(XAResource epXAR, SimulatedMessageEndpointFactory owningFactory) {
        this.epXAR = epXAR;
        this.owningFactory = owningFactory;
    }

    public void beforeDelivery(Method meth) throws NoSuchMethodException, ResourceException {
        this.generatedXid = XidImpl.getXidInstanceForLocalTransaction(
                NativeXAFileSystem.getXAFileSystem().getNextLocalTransactionId());
        try {
            epXAR.start(generatedXid, XAResource.TMNOFLAGS);
        } catch (XAException xae) {
            xae.printStackTrace();
            throw new ResourceException(xae);
        }
    }

    public void onFileSystemEvent(FileStateChangeEvent event) {
        System.out.println("Received " + event.getEventType() + " event for file " + event.getFile());
        if (this.goTill == SimulatedMessageEndpointFactory.GoTill.consume) {
            throw new ArrayIndexOutOfBoundsException();
        }
        owningFactory.incrementEventsReceivedCount();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
        }
    }

    public void afterDelivery() throws ResourceException {
        try {
            epXAR.end(generatedXid, XAResource.TMSUCCESS);
            epXAR.prepare(generatedXid);
            if (this.goTill == SimulatedMessageEndpointFactory.GoTill.prepare) {
                return;
            }
            epXAR.commit(generatedXid, false);
        } catch (XAException xae) {
            throw new ResourceException(xae);
        }
    }

    public void release() {
    }
}

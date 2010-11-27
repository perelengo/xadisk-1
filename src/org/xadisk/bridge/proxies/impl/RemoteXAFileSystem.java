package org.xadisk.bridge.proxies.impl;

import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import java.io.IOException;
import java.io.Serializable;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.bridge.proxies.facilitators.RemoteXADiskActivationSpecImpl;
import org.xadisk.connector.inbound.EndPointActivation;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.server.conversation.HostedContext;
import org.xadisk.filesystem.NativeXAFileSystem;

public class RemoteXAFileSystem extends RemoteObjectProxy implements XAFileSystem {

    private static final long serialVersionUID = 1L;
    
    public RemoteXAFileSystem(String serverAddress, int serverPort) {
        super(0, new RemoteMethodInvoker(serverAddress, serverPort));
    }

    public RemoteXAFileSystem(long objectId, RemoteMethodInvoker invoker) {
        super(objectId, invoker);
    }

    public Session createSessionForLocalTransaction() {
        try {
            return (Session) invokeRemoteMethod("createSessionForLocalTransaction");
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public Session createSessionForXATransaction(Xid xid) {
        try {
            return (Session) invokeRemoteMethod("createSessionForXATransaction", (Serializable) xid);
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public int getDefaultTransactionTimeout() {
        try {
            return (Integer) invokeRemoteMethod("getDefaultTransactionTimeout");
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public Session getSessionForTransaction(Xid xid) {
        try {
            return (Session) invokeRemoteMethod("getSessionForTransaction", (Serializable) xid);
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void notifySystemFailureAndContinue(Throwable t) {
        try {
            invokeRemoteMethod("notifySystemFailureAndContinue", t);
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public Xid[] recover(int flag) throws XAException {
        try {
            return (Xid[]) invokeRemoteMethod("recover", flag);
        } catch (XAException xae) {
            throw xae;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void shutdown() {
        try {
            this.invoker.disconnect();
        } catch (IOException ioe) {
            //no-op.
        }
    }

    public void waitForBootup(long timeout) throws InterruptedException {
        try {
            invokeRemoteMethod("waitForBootup", timeout);
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void registerEndPointActivation(EndPointActivation epActivation) throws IOException {
        try {
            MessageEndpointFactory messageEndpointFactory = epActivation.getMessageEndpointFactory();
            HostedContext globalCallbackContext = NativeXAFileSystem.getXAFileSystem().getGlobalCallbackContext();
            long objectId = globalCallbackContext.hostObject(messageEndpointFactory);
            String xaDiskSystemId = NativeXAFileSystem.getXAFileSystem().getXADiskSystemId();
            RemoteMessageEndpointFactory remoteMessageEndpointFactory = new RemoteMessageEndpointFactory(objectId, xaDiskSystemId,
                    NativeXAFileSystem.getXAFileSystem().createRemoteMethodInvokerToSelf());

            RemoteXADiskActivationSpecImpl ras = new RemoteXADiskActivationSpecImpl(epActivation.getActivationSpecImpl());
            EndPointActivation callbackEndPointActivation = new EndPointActivation(remoteMessageEndpointFactory,
                    ras);
            invokeRemoteMethod("registerEndPointActivation", callbackEndPointActivation);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void deRegisterEndPointActivation(EndPointActivation epActivation) throws IOException {
        try {
            MessageEndpointFactory messageEndpointFactory = epActivation.getMessageEndpointFactory();
            HostedContext globalCallbackContext = NativeXAFileSystem.getXAFileSystem().getGlobalCallbackContext();
            long objectId = globalCallbackContext.deHostObject(messageEndpointFactory);
            String xaDiskSystemId = NativeXAFileSystem.getXAFileSystem().getXADiskSystemId();
            RemoteMessageEndpointFactory remoteMessageEndpointFactory =
                    new RemoteMessageEndpointFactory(objectId, xaDiskSystemId, null);

            RemoteXADiskActivationSpecImpl ras = new RemoteXADiskActivationSpecImpl(epActivation.getActivationSpecImpl());
            EndPointActivation callbackEndPointActivation = new EndPointActivation(remoteMessageEndpointFactory, ras);
            invokeRemoteMethod("deRegisterEndPointActivation", callbackEndPointActivation);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public XAResource getEventProcessingXAResourceForRecovery() {
        try {
            return (XAResource) invokeRemoteMethod("getEventProcessingXAResourceForRecovery");
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }
}

package org.xadisk.bridge.proxies.impl;

import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import java.io.IOException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;
import org.xadisk.connector.inbound.EndPointActivation;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.server.conversation.HostedContext;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.XidImpl;

public class RemoteXAFileSystem extends RemoteObjectProxy implements XAFileSystem {

    public RemoteXAFileSystem(String serverAddress, int serverPort) throws IOException {
        super(0, new RemoteMethodInvoker(serverAddress, serverPort).ensureConnected());
    }

    public RemoteXAFileSystem(int objectId, RemoteMethodInvoker invoker) {
        super(objectId, invoker);
    }

    public Session createSessionForLocalTransaction() {
        try {
            return (Session) invokeRemoteMethod("createSessionForLocalTransaction");
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public Session createSessionForXATransaction(XidImpl xid) {
        try {
            return (Session) invokeRemoteMethod("createSessionForXATransaction", xid);
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

    public Session getSessionForTransaction(XidImpl xid) {
        try {
            return (Session) invokeRemoteMethod("getSessionForTransaction", xid);
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void notifySystemFailureAndContinue(Throwable t) {
        try {
            invokeRemoteMethod("notifySystemFailureAndContinue", t);
        } catch (Throwable th) {
            throw assertExceptionHandling(t);
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
        } catch(IOException ioe) {
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

    public void registerEndPointActivation(EndPointActivation epActivation) {
        try {
            MessageEndpointFactory messageEndpointFactory = epActivation.getMessageEndpointFactory();
            HostedContext globalCallbackContext = NativeXAFileSystem.getXAFileSystem().getGlobalCallbackContext();
            int objectId = globalCallbackContext.hostObject(messageEndpointFactory);
            int xaDiskSystemId = NativeXAFileSystem.getXAFileSystem().getXADiskSystemId();
            RemoteMessageEndpointFactory remoteMessageEndpointFactory = new RemoteMessageEndpointFactory(objectId, xaDiskSystemId,
                    NativeXAFileSystem.getXAFileSystem().createRemoteMethodInvokerToSelf());

            EndPointActivation callbackEndPointActivation = new EndPointActivation(remoteMessageEndpointFactory,
                    epActivation.getActivationSpecImpl());
            invokeRemoteMethod("registerEndPointActivation", callbackEndPointActivation);
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void deRegisterEndPointActivation(EndPointActivation epActivation) {
        try {
            MessageEndpointFactory messageEndpointFactory = epActivation.getMessageEndpointFactory();
            HostedContext globalCallbackContext = NativeXAFileSystem.getXAFileSystem().getGlobalCallbackContext();
            int objectId = globalCallbackContext.deHostObject(messageEndpointFactory);
            if (objectId == 1) {
                throw new AssertionError();
            }
            int xaDiskSystemId = NativeXAFileSystem.getXAFileSystem().getXADiskSystemId();
            RemoteMessageEndpointFactory remoteMessageEndpointFactory =
                    new RemoteMessageEndpointFactory(objectId, xaDiskSystemId, null);

            EndPointActivation callbackEndPointActivation = new EndPointActivation(remoteMessageEndpointFactory,
                    epActivation.getActivationSpecImpl());
            invokeRemoteMethod("deRegisterEndPointActivation", callbackEndPointActivation);
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }
}

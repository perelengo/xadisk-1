package org.xadisk.bridge.proxies.impl;

import java.io.IOException;
import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import org.xadisk.bridge.proxies.facilitators.SerializedMethod;
import java.lang.reflect.Method;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import org.xadisk.bridge.proxies.facilitators.MethodSerializabler;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.bridge.server.conversation.HostedContext;

public class RemoteMessageEndpointFactory extends RemoteObjectProxy implements MessageEndpointFactory {

    private final String xaDiskSystemId;

    public RemoteMessageEndpointFactory(int objectId, String xaDiskSystemId, RemoteMethodInvoker invoker) {
        super(objectId, invoker);
        this.xaDiskSystemId = xaDiskSystemId;
    }

    synchronized public boolean isDeliveryTransacted(Method method) throws NoSuchMethodException {
        try {
            SerializedMethod serializableMethod = new MethodSerializabler().serialize(method);
            return (Boolean) invokeRemoteMethod("isDeliveryTransacted", serializableMethod);
        } catch (NoSuchMethodException nsme) {
            throw nsme;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    synchronized public MessageEndpoint createEndpoint(XAResource xar) throws UnavailableException {
        try {
            HostedContext globalCallbackContext = NativeXAFileSystem.getXAFileSystem().getGlobalCallbackContext();
            int objectId = globalCallbackContext.hostObject(xar);
            RemoteEventProcessingXAResource remoteEventProcessingXAResource = new RemoteEventProcessingXAResource(objectId,
                    NativeXAFileSystem.getXAFileSystem().createRemoteMethodInvokerToSelf());
            RemoteMessageEndpoint remoteMEP =
                    (RemoteMessageEndpoint) invokeRemoteMethod("createEndpoint", remoteEventProcessingXAResource);
            remoteMEP.setInvoker((RemoteMethodInvoker) this.invoker.clone());
            return remoteMEP;
        } catch (UnavailableException ue) {
            throw ue;
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RemoteMessageEndpointFactory) {
            RemoteMessageEndpointFactory that = (RemoteMessageEndpointFactory) obj;
            return this.remoteObjectId == that.remoteObjectId
                    && this.xaDiskSystemId.equals(that.xaDiskSystemId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return xaDiskSystemId.hashCode() + remoteObjectId;
    }
}

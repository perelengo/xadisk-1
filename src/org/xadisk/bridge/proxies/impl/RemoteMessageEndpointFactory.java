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

    private static final long serialVersionUID = 1L;

    private final String xaDiskSystemId;

    public RemoteMessageEndpointFactory(long objectId, String xaDiskSystemId, RemoteMethodInvoker invoker) {
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
            long objectId = globalCallbackContext.hostObject(xar);
            //one problem was to deHost this xar at an appropriate time when its use is done. But its
            //use may not be done with the end of mep.release(). Who knows, the remote TM sends txn
            //related commands to this xar at some point quite later. No. Not so. Good news came from
            //the JCA spec: "During the afterDelivery call from the resource adapter, the application server
            //completes the transaction and sends transaction completion notifications to the
            //XAResource instance".
            RemoteEventProcessingXAResource remoteEventProcessingXAResource = new RemoteEventProcessingXAResource(objectId,
                    NativeXAFileSystem.getXAFileSystem().createRemoteMethodInvokerToSelf());
            RemoteMessageEndpoint remoteMEP =
                    (RemoteMessageEndpoint) invokeRemoteMethod("createEndpoint", remoteEventProcessingXAResource);
            remoteMEP.setInvoker((RemoteMethodInvoker) this.invoker.makeCopy());
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

    /*
     * JCA spec dis-allows equals method for this bean. For this case,
     * remoteObjectId infact follows the remote unique object identities for the
     * original MEF objects.
     */
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
        return xaDiskSystemId.hashCode() + (int) remoteObjectId;
    }

    public String getXaDiskSystemId() {
        return xaDiskSystemId;
    }

    public long getRemoteObjectId() {
        return remoteObjectId;
    }

    public RemoteMethodInvoker getInvoker() {
        return invoker;
    }
}

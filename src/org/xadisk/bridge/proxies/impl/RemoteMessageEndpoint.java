package org.xadisk.bridge.proxies.impl;

import java.io.IOException;
import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import java.lang.reflect.Method;
import javax.resource.ResourceException;
import javax.resource.spi.endpoint.MessageEndpoint;
import org.xadisk.bridge.proxies.facilitators.MethodSerializabler;
import org.xadisk.bridge.proxies.facilitators.SerializedMethod;
import org.xadisk.connector.inbound.FileSystemEventListener;
import org.xadisk.filesystem.FileStateChangeEvent;

public class RemoteMessageEndpoint extends RemoteObjectProxy implements MessageEndpoint, FileSystemEventListener {

    public RemoteMessageEndpoint(int objectId, RemoteMethodInvoker invoker) {
        super(objectId, invoker);
    }

    public void beforeDelivery(Method method) throws NoSuchMethodException, ResourceException {
        try {
            SerializedMethod serializableMethod = new MethodSerializabler().serialize(method);
            invokeRemoteMethod("beforeDelivery", serializableMethod);
        } catch (NoSuchMethodException nsme) {
            throw nsme;
        } catch (ResourceException re) {
            throw re;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void onFileSystemEvent(FileStateChangeEvent event) {
        try {
            invokeRemoteMethod("onFileSystemEvent", event);
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void afterDelivery() throws ResourceException {
        try {
            invokeRemoteMethod("afterDelivery");
        } catch (ResourceException re) {
            throw re;
        } catch (Throwable th) {
            throw assertExceptionHandling(th);
        }
    }

    public void release() {
        try {
            invokeRemoteMethod("release");
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
}

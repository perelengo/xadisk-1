package org.xadisk.bridge.proxies.facilitators;

import java.io.Serializable;

public class RemoteObjectProxy implements Serializable {
    
    private static final long serialVersionUID = 1L;

    protected final long remoteObjectId;
    protected RemoteMethodInvoker invoker;

    public RemoteObjectProxy(long remoteObjectId, RemoteMethodInvoker invoker) {
        this.remoteObjectId = remoteObjectId;
        this.invoker = invoker;
    }

    public void setInvoker(RemoteMethodInvoker invoker) {
        this.invoker = invoker;
    }
    
    protected RuntimeException assertExceptionHandling(Throwable t) {
        if(t instanceof RuntimeException) {
            return (RuntimeException)t;
        } else {
            throw new AssertionError(t);
        }
    }

    protected Object invokeRemoteMethod(String methodName, Serializable... args) throws Throwable {
        Object response = invoker.invokeRemoteMethod(remoteObjectId, methodName, args);
        if(response instanceof RemoteObjectProxy) {
            ((RemoteObjectProxy)response).setInvoker(this.invoker);
        }
        return response;
    }
}

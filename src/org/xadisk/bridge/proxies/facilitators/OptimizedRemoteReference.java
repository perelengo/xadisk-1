package org.xadisk.bridge.proxies.facilitators;

import java.io.Serializable;

public abstract class OptimizedRemoteReference<R> implements Serializable {
    
    public abstract R regenerateRemoteObject();

    public abstract void setResultObject(R resultObject);

    public abstract void mergeWithRemoteObject(R resultObject);

    public abstract R getResultObject();
}
package org.xadisk.bridge.proxies.impl;

import java.io.File;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import org.xadisk.bridge.proxies.interfaces.Lock;

public class RemoteLock extends RemoteObjectProxy implements Lock {

    private static final long serialVersionUID = 1L;
    private final File resource;
    private boolean exclusive;

    public RemoteLock(long objectId, File resource, boolean exclusive) {
        super(objectId, null);
        this.resource = resource;
        this.exclusive = exclusive;
    }

    public File getResource() {
        return resource;
    }

    public boolean isExclusive() {
        return exclusive;
    }
}
package org.xadisk.bridge.server.conversation;

public interface HostedContext {

    public long hostObject(Object target);

    public void deHostObjectWithId(long objectId);

    public long deHostObject(Object target);

    public Object getHostedObjectWithId(long objectId);
}

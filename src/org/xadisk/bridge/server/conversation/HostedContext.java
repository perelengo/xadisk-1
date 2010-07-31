package org.xadisk.bridge.server.conversation;

public interface HostedContext {

    public int hostObject(Object target);

    public void deHostObjectWithId(int objectId);

    public int deHostObject(Object target);

    public Object getHostedObjectWithId(int objectId);
}

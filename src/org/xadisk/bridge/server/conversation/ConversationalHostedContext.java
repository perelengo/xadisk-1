package org.xadisk.bridge.server.conversation;

import java.util.ArrayList;

public class ConversationalHostedContext implements HostedContext {
    
    private final ArrayList<Object> remoteInvocationTargets = new ArrayList<Object>();

    public int hostObject(Object target) {
        remoteInvocationTargets.add(target);
        return remoteInvocationTargets.size() - 1;
    }

    public Object getHostedObjectWithId(int objectId) {
        return remoteInvocationTargets.get(objectId);
    }

    public int deHostObject(Object target) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void deHostObjectWithId(int objectId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

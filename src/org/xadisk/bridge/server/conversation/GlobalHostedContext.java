package org.xadisk.bridge.server.conversation;

import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GlobalHostedContext implements HostedContext {

    private final ConcurrentHashMap<Integer, Object> remoteInvocationTargets =
            new ConcurrentHashMap<Integer, Object>();
    private final AtomicInteger objectIdSequence = new AtomicInteger(0);

    //-ve IDs are for global contexts, +ve/0 are for local (conversation specific) contexts.
    public int hostObject(Object target) {
        int objectId = objectIdSequence.decrementAndGet();
        remoteInvocationTargets.put(objectId, target);
        return objectId;
    }

    public void deHostObjectWithId(int objectId) {
        remoteInvocationTargets.remove(objectId);
    }

    public int deHostObject(Object target) {
        Integer objectId = null;
        Set<Entry<Integer, Object>> entries = remoteInvocationTargets.entrySet();
        for (Entry<Integer, Object> entry : entries) {
            if (entry.getValue().equals(target)) {
                objectId = entry.getKey();
                break;
            }
        }
        if (objectId != null) {
            remoteInvocationTargets.remove(objectId);
            return objectId;
        }
        return 1;
    }

    public Object getHostedObjectWithId(int objectId) {
        return remoteInvocationTargets.get(objectId);
    }
}

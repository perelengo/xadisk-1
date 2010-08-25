package org.xadisk.bridge.server.conversation;

import org.xadisk.filesystem.pools.PooledSelector;
import org.xadisk.filesystem.pools.SelectorPool;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Set;
import javax.resource.spi.work.Work;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.bridge.proxies.facilitators.ByteArrayRemoteReference;
import org.xadisk.bridge.proxies.facilitators.MethodSerializabler;
import org.xadisk.bridge.proxies.facilitators.OptimizedRemoteReference;
import org.xadisk.bridge.proxies.facilitators.SerializedMethod;
import org.xadisk.filesystem.exceptions.ContextOutOfSyncException;

public class RemoteMethodInvocationHandler implements Work {

    private ConversationContext context;
    private static final String UTF8CharsetName = "UTF-8";
    private final PooledSelector pooledWriteSelector;
    private final Selector writeSelector;
    private volatile boolean enabled = true;
    private final SelectorPool selectorPool;

    public RemoteMethodInvocationHandler(ConversationContext context) throws IOException {
        this.context = context;
        this.selectorPool = NativeXAFileSystem.getXAFileSystem().getSelectorPool();
        this.pooledWriteSelector = selectorPool.checkOut();
        if (pooledWriteSelector == null) {
            throw new IOException("Could not get a Selector from the SelectorPool.");
        }
        this.writeSelector = pooledWriteSelector.getSelector();
    }

    public void run() {
        byte[] methodInvocationResponse;
        try {
            context.reAssociatedTransactionThreadWithSessions(Thread.currentThread());
            methodInvocationResponse = handleRemoteInvocation(context.getCurrentMethodInvocation());
        } catch (Throwable t) {
            methodInvocationResponse = handleInvocationFailedSystemError(t);
        }
        try {
            SocketChannel channel = context.getConversationChannel();
            channel.register(writeSelector, SelectionKey.OP_WRITE);
            ByteBuffer toSend = ByteBuffer.wrap(methodInvocationResponse);
            while (toSend.remaining() > 0 && enabled) {
                int n = writeSelector.select();
                if (n == 0) {
                    //release() got called...thats the only possibility.
                    break;
                }
                Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
                //this will be only one key always, the above one.
                channel.write(toSend);
                selectionKeys.clear();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            //can't do anything here...can't even send the "t" to the remote client itself, as the error
            //above is related to sending (though something else) only.
        } finally {
            try {
                selectorPool.checkIn(pooledWriteSelector);
            } catch (Throwable t) {
                //no-op.
            }
        }
    }

    private byte[] handleInvocationFailedSystemError(Throwable t) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeBoolean(true);
            oos.writeInt(1);
            oos.writeObject(t);
            return baos.toByteArray();
        } catch (Throwable th) {
            throw new AssertionError(th);
        }
    }

    byte[] handleRemoteInvocation(byte[] invocation) throws IOException,
            ClassNotFoundException,
            NoSuchMethodException,
            IllegalAccessException,
            ContextOutOfSyncException {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(invocation));

        long targetObjectId = ois.readLong();
        Object targetObject = context.getInvocationTarget(targetObjectId);
        if (targetObject == null) {
            throw new ContextOutOfSyncException("No object with id " + targetObjectId);
        }

        byte[] methodNameBytes = new byte[ois.readInt()];
        ois.read(methodNameBytes);
        String methodName = new String(methodNameBytes, UTF8CharsetName);

        int numOfArgs = ois.readInt();
        Object args[] = new Object[numOfArgs];
        Class argTypes[] = new Class[numOfArgs];
        ArrayList<OptimizedRemoteReference> remoteReferences = new ArrayList<OptimizedRemoteReference>();
        ArrayList<Object> regenerateObjects = new ArrayList<Object>();
        for (int i = 0; i < numOfArgs; i++) {
            args[i] = ois.readObject();
            argTypes[i] = args[i].getClass();
            if (args[i] instanceof OptimizedRemoteReference) {
                OptimizedRemoteReference remoteRef = (OptimizedRemoteReference) args[i];
                args[i] = remoteRef.regenerateRemoteObject();
                regenerateObjects.add(args[i]);
                remoteReferences.add(remoteRef);
            }
            if (args[i] instanceof SerializedMethod) {
                SerializedMethod serializedMethod = (SerializedMethod) args[i];
                args[i] = new MethodSerializabler().reconstruct(serializedMethod);
                argTypes[i] = Method.class;
            }
            argTypes[i] = getApplicableInterfaceClassIfRequired(args[i]);
            argTypes[i] = getPrimitiveClassIfRequired(argTypes[i]);
        }
        Method method = targetObject.getClass().getMethod(methodName, argTypes);
        boolean isError = false;
        Object response;
        try {
            response = method.invoke(targetObject, args);
            for (int i = 0; i < remoteReferences.size(); i++) {
                OptimizedRemoteReference ref = remoteReferences.get(i);
                if (ref instanceof ByteArrayRemoteReference) {
                    Integer bytesGotUpdated = (Integer) response;
                    byte[] inputArgument = (byte[]) regenerateObjects.get(i);
                    byte[] minimalToSend = new byte[bytesGotUpdated];
                    System.arraycopy(inputArgument, 0, minimalToSend, 0, bytesGotUpdated);
                    ref.setResultObject(minimalToSend);
                }
            }
            //note that in case of method thrown exception, the remote arguments are not geting updated. That would be fine for current applications.
            response = context.convertToProxyResponseIfRequired(response);
        } catch (InvocationTargetException ite) {
            response = ite.getCause();
            isError = true;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeBoolean(isError);
        oos.writeInt(1 + remoteReferences.size());
        oos.writeObject(response);
        for (OptimizedRemoteReference ref : remoteReferences) {
            oos.writeObject(ref);
        }
        return baos.toByteArray();
    }

    public void release() {
        enabled = false;
        if (writeSelector.isOpen()) {
            writeSelector.wakeup();
        }
    }

    private Class getPrimitiveClassIfRequired(Class type) {
        if (type == Integer.class) {
            return int.class;
        }
        if (type == Long.class) {
            return long.class;
        }
        if (type == Byte.class) {
            return byte.class;
        }
        if (type == Boolean.class) {
            return boolean.class;
        }
        if (type == Short.class) {
            return short.class;
        }
        if (type == Character.class) {
            return char.class;
        }
        if (type == Float.class) {
            return float.class;
        }
        if (type == Double.class) {
            return double.class;
        }
        return type;
    }

    private Class getApplicableInterfaceClassIfRequired(Object obj) {
        if (obj instanceof XAResource) {
            return XAResource.class;
            //this was to do avoid a strange/unexpected error saying NoSuchMethodException in cases
            //when the parameter-type (during method invocation via reflection) was specified as an
            //implementing class name instead of the interface.
        }
        if (obj instanceof Xid) {
            return Xid.class;//reason same as above.
        }
        return obj.getClass();
    }
}

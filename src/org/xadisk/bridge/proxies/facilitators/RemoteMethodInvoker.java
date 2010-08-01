package org.xadisk.bridge.proxies.facilitators;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import org.xadisk.filesystem.exceptions.ConnectionException;
import org.xadisk.filesystem.exceptions.XASystemException;

public class RemoteMethodInvoker implements Serializable {

    private final String serverAddress;
    private final int serverPort;
    private transient SocketChannel channel;
    private transient Socket socket;
    private boolean connected = false;
    private static final String UTF8CharsetName = "UTF-8";

    public RemoteMethodInvoker(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new RemoteMethodInvoker(serverAddress, serverPort);
    }

    public RemoteMethodInvoker ensureConnected() throws IOException {
        if (connected) {
            return this;
        }
        channel = SocketChannel.open(new InetSocketAddress(serverAddress, serverPort));
        channel.configureBlocking(true);
        channel.finishConnect();
        socket = channel.socket();
        connected = true;
        return this;
    }

    public void disconnect() throws IOException {
        if (connected) {
            socket.close();
            connected = false;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public Object invokeRemoteMethod(int targetObjectId, String method, Serializable... args) throws XASystemException, Throwable {
        boolean isError;
        Object returnObject;
        try {
            ensureConnected();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeInt(targetObjectId);
            byte[] methodNameBytes = method.getBytes(UTF8CharsetName);
            oos.writeInt(methodNameBytes.length);
            oos.write(methodNameBytes);
            oos.writeInt(args.length);
            ArrayList<OptimizedRemoteReference> remoteReferences = new ArrayList<OptimizedRemoteReference>();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof OptimizedRemoteReference) {
                    remoteReferences.add((OptimizedRemoteReference) args[i]);
                }
                oos.writeObject(args[i]);
            }
            oos.flush();

            byte[] toSend = baos.toByteArray();
            int lengthOfInvocation = toSend.length;

            OutputStream socketOS = socket.getOutputStream();
            socketOS.write(getDataOutputCompliantBytesFromInteger(lengthOfInvocation));
            socketOS.write(toSend);
            socketOS.flush();

            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            isError = ois.readBoolean();
            int numOutputs = ois.readInt();
            returnObject = ois.readObject();
            for (int i = 1; i < numOutputs; i++) {
                OptimizedRemoteReference updatedRef = (OptimizedRemoteReference) ois.readObject();
                remoteReferences.get(i - 1).mergeWithRemoteObject(updatedRef.getResultObject());
            }
        } catch (IOException ioe) {
            this.disconnect();
            throw new ConnectionException(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new XASystemException(cnfe);
        }
        if (isError) {
            throw (Throwable) returnObject;
        }
        return returnObject;
    }

    private byte[] getDataOutputCompliantBytesFromInteger(int i) {
        byte[] b = new byte[4];
        b[0] = (byte) ((i >> 24) & 0xFF);
        b[1] = (byte) ((i >> 16) & 0xFF);
        b[2] = (byte) ((i >> 8) & 0xFF);
        b[3] = (byte) (i & 0xFF);
        return b;
    }
}
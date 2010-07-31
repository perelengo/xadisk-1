package org.xadisk.bridge.proxies.facilitators;

public class ByteArrayRemoteReference extends OptimizedRemoteReference<byte[]> {

    private transient byte[] originalByteArray;
    private int lengthForUpdate;
    private transient int offsetForUpdate;
    private byte[] resultBytes;

    public ByteArrayRemoteReference(byte[] b, int offset, int length) {
        this.originalByteArray = b;
        this.lengthForUpdate = length;
        this.offsetForUpdate = offset;
    }

    public byte[] regenerateRemoteObject() {
        return new byte[lengthForUpdate];
    }

    public void setResultObject(byte[] b) {
        resultBytes = b;
    }

    public void mergeWithRemoteObject(byte[] resultBytes) {
        byte[] result = resultBytes;
        System.arraycopy(result, 0, originalByteArray, offsetForUpdate, result.length);
    }

    public byte[] getResultObject() {
        return resultBytes;
    }
}

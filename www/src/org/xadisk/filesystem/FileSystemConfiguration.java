package org.xadisk.filesystem;

import java.io.Serializable;

public class FileSystemConfiguration implements Serializable {

    private int directBufferPoolSize = 1000;
    private int nonDirectBufferPoolSize = 1000;
    private int bufferSize = 4096;
    private String xaDiskHome;
    private long transactionLogFileMaxSize = 1000000000;
    private int cumulativeBufferSizeForDiskWrite = 1000000;
    private int directBufferIdleTime = 100;
    private int nonDirectBufferIdleTime = 100;
    private int bufferPoolRelieverInterval = 60;
    private long maxNonPooledBufferSize = 2 ^ 20;
    private int deadLockDetectorInterval = 30;
    private int lockTimeOut = 10000;
    private int maximumConcurrentEventDeliveries = 20;
    private int logLevel = 3;//informational.
    private int transactionTimeout = 60;

    public FileSystemConfiguration() {
    }

    FileSystemConfiguration(String xaDiskHome) {
        this.xaDiskHome = xaDiskHome;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public int getDirectBufferPoolSize() {
        return directBufferPoolSize;
    }

    public void setDirectBufferPoolSize(int directBufferPoolSize) {
        this.directBufferPoolSize = directBufferPoolSize;
    }

    public int getNonDirectBufferPoolSize() {
        return nonDirectBufferPoolSize;
    }

    public void setNonDirectBufferPoolSize(int nonDirectBufferPoolSize) {
        this.nonDirectBufferPoolSize = nonDirectBufferPoolSize;
    }

    public String getXaDiskHome() {
        return xaDiskHome;
    }

    public void setXaDiskHome(String xaDiskHome) {
        this.xaDiskHome = xaDiskHome;
    }

    public long getTransactionLogFileMaxSize() {
        return transactionLogFileMaxSize;
    }

    public void setTransactionLogFileMaxSize(long transactionLogFileMaxSize) {
        this.transactionLogFileMaxSize = transactionLogFileMaxSize;
    }

    public int getCumulativeBufferSizeForDiskWrite() {
        return cumulativeBufferSizeForDiskWrite;
    }

    public void setCumulativeBufferSizeForDiskWrite(int cumulativeBufferSizeForDiskWrite) {
        this.cumulativeBufferSizeForDiskWrite = cumulativeBufferSizeForDiskWrite;
    }

    public int getDirectBufferIdleTime() {
        return directBufferIdleTime;
    }

    public int getNonDirectBufferIdleTime() {
        return nonDirectBufferIdleTime;
    }

    public void setDirectBufferIdleTime(int directBufferIdleTime) {
        this.directBufferIdleTime = directBufferIdleTime;
    }

    public void setNonDirectBufferIdleTime(int nonDirectBufferIdleTime) {
        this.nonDirectBufferIdleTime = nonDirectBufferIdleTime;
    }

    public int getBufferPoolRelieverInterval() {
        return bufferPoolRelieverInterval;
    }

    public void setBufferPoolRelieverInterval(int bufferPoolRelieverInterval) {
        this.bufferPoolRelieverInterval = bufferPoolRelieverInterval;
    }

    public long getMaxNonPooledBufferSize() {
        return maxNonPooledBufferSize;
    }

    public void setMaxNonPooledBufferSize(long maxNonPooledBufferSize) {
        this.maxNonPooledBufferSize = maxNonPooledBufferSize;
    }

    public int getDeadLockDetectorInterval() {
        return deadLockDetectorInterval;
    }

    public void setDeadLockDetectorInterval(int deadLockDetectorInterval) {
        this.deadLockDetectorInterval = deadLockDetectorInterval;
    }

    public int getLockTimeOut() {
        return lockTimeOut;
    }

    public void setLockTimeOut(int lockTimeOut) {
        this.lockTimeOut = lockTimeOut;
    }

    public int getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public int getMaximumConcurrentEventDeliveries() {
        return maximumConcurrentEventDeliveries;
    }

    public void setMaximumConcurrentEventDeliveries(int maximumConcurrentEventDeliveries) {
        this.maximumConcurrentEventDeliveries = maximumConcurrentEventDeliveries;
    }

    public int getTransactionTimeout() {
        return transactionTimeout;
    }

    public void setTransactionTimeout(int transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
    }
}

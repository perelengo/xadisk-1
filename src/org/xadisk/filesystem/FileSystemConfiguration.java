package org.xadisk.filesystem;

import java.io.Serializable;

/**
 * An object of this class encapsulates the configuration for XADisk, and is used while
 * booting of the XADisk instance. When booting from standalone Java programs, a subclass
 * object of type "StandaloneFileSystemConfiguration" needs to be used.
 *
 * For details on all of these properties, please refer to the User Guide.
 */
public class FileSystemConfiguration implements Serializable {

    private Integer directBufferPoolSize = 1000;
    private Integer nonDirectBufferPoolSize = 1000;
    private Integer bufferSize = 4096;
    private String xaDiskHome;
    private Long transactionLogFileMaxSize = 1000000000L;
    private Integer cumulativeBufferSizeForDiskWrite = 1000000;
    private Integer directBufferIdleTime = 100;
    private Integer nonDirectBufferIdleTime = 100;
    private Integer bufferPoolRelieverInterval = 60;
    private Long maxNonPooledBufferSize = 1000000L;
    private Integer deadLockDetectorInterval = 30;
    private Integer lockTimeOut = 10000;
    private Integer maximumConcurrentEventDeliveries = 20;
    private Integer logLevel = 3;//informational.
    private Integer transactionTimeout = 60;
    private String serverAddress = "127.0.0.1";
    private Integer serverPort = 9999;

    public FileSystemConfiguration() {
    }

    public FileSystemConfiguration(String xaDiskHome) {
        this.xaDiskHome = xaDiskHome;
    }

    public Integer getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(Integer bufferSize) {
        this.bufferSize = bufferSize;
    }

    public Integer getDirectBufferPoolSize() {
        return directBufferPoolSize;
    }

    public void setDirectBufferPoolSize(Integer directBufferPoolSize) {
        this.directBufferPoolSize = directBufferPoolSize;
    }

    public Integer getNonDirectBufferPoolSize() {
        return nonDirectBufferPoolSize;
    }

    public void setNonDirectBufferPoolSize(Integer nonDirectBufferPoolSize) {
        this.nonDirectBufferPoolSize = nonDirectBufferPoolSize;
    }

    public String getXaDiskHome() {
        return xaDiskHome;
    }

    public void setXaDiskHome(String xaDiskHome) {
        this.xaDiskHome = xaDiskHome;
    }

    public Long getTransactionLogFileMaxSize() {
        return transactionLogFileMaxSize;
    }

    public void setTransactionLogFileMaxSize(Long transactionLogFileMaxSize) {
        this.transactionLogFileMaxSize = transactionLogFileMaxSize;
    }

    public Integer getCumulativeBufferSizeForDiskWrite() {
        return cumulativeBufferSizeForDiskWrite;
    }

    public void setCumulativeBufferSizeForDiskWrite(Integer cumulativeBufferSizeForDiskWrite) {
        this.cumulativeBufferSizeForDiskWrite = cumulativeBufferSizeForDiskWrite;
    }

    public Integer getDirectBufferIdleTime() {
        return directBufferIdleTime;
    }

    public Integer getNonDirectBufferIdleTime() {
        return nonDirectBufferIdleTime;
    }

    public void setDirectBufferIdleTime(Integer directBufferIdleTime) {
        this.directBufferIdleTime = directBufferIdleTime;
    }

    public void setNonDirectBufferIdleTime(Integer nonDirectBufferIdleTime) {
        this.nonDirectBufferIdleTime = nonDirectBufferIdleTime;
    }

    public Integer getBufferPoolRelieverInterval() {
        return bufferPoolRelieverInterval;
    }

    public void setBufferPoolRelieverInterval(Integer bufferPoolRelieverInterval) {
        this.bufferPoolRelieverInterval = bufferPoolRelieverInterval;
    }

    public Long getMaxNonPooledBufferSize() {
        return maxNonPooledBufferSize;
    }

    public void setMaxNonPooledBufferSize(Long maxNonPooledBufferSize) {
        this.maxNonPooledBufferSize = maxNonPooledBufferSize;
    }

    public Integer getDeadLockDetectorInterval() {
        return deadLockDetectorInterval;
    }

    public void setDeadLockDetectorInterval(Integer deadLockDetectorInterval) {
        this.deadLockDetectorInterval = deadLockDetectorInterval;
    }

    public Integer getLockTimeOut() {
        return lockTimeOut;
    }

    public void setLockTimeOut(Integer lockTimeOut) {
        this.lockTimeOut = lockTimeOut;
    }

    public Integer getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(Integer logLevel) {
        this.logLevel = logLevel;
    }

    public Integer getMaximumConcurrentEventDeliveries() {
        return maximumConcurrentEventDeliveries;
    }

    public void setMaximumConcurrentEventDeliveries(Integer maximumConcurrentEventDeliveries) {
        this.maximumConcurrentEventDeliveries = maximumConcurrentEventDeliveries;
    }

    public Integer getTransactionTimeout() {
        return transactionTimeout;
    }

    public void setTransactionTimeout(Integer transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }
}

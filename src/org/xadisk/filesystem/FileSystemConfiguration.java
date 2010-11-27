package org.xadisk.filesystem;

import java.io.Serializable;

/**
 * An object of this class encapsulates the configuration for XADisk, and is used while
 * booting of the XADisk instance. When booting from standalone Java programs, a subclass
 * object of type "StandaloneFileSystemConfiguration" needs to be used.
 */

public class FileSystemConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;
    
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

    /**
     * A constructor called by JavaEE Containers. This default constructor is required for this class to be a Java Bean.
     */
    public FileSystemConfiguration() {
    }

    /**
     * A constructor called by constructor of subclass StandaloneFileSystemConfiguration.
     * @param xaDiskHome
     */
    public FileSystemConfiguration(String xaDiskHome) {
        this.xaDiskHome = xaDiskHome;
    }

    /**
     * Returns the value of bufferSize.
     * A performance tuning property. XADisk tries to hold its ongoing transactions' logs in memory
     * using byte buffers (these are not pooled). Such byte buffers are also used for normal file reading and
     * writing if the pooled buffers are all exhausted. As the total memory consumption can significantly
     * increase due to these buffers, this property is provided by XADisk to put an
     * upper limit on total size of in-memory buffers (non-pooled). Default value is 1000000.
     * @return bufferSize
     */
    public Integer getBufferSize() {
        return bufferSize;
    }

    /**
     * Sets the value of bufferSize.
     * A performance tuning property. XADisk tries to hold its ongoing transactions' logs in memory
     * using byte buffers (these are not pooled). Such byte buffers are also used for normal file reading and
     * writing if the pooled buffers are all exhausted. As the total memory consumption can significantly
     * increase due to these buffers, this property is provided by XADisk to put an
     * upper limit on total size of in-memory buffers (non-pooled). Default value is 1000000.
     * @param bufferSize new value of bufferSize.
     */
    public void setBufferSize(Integer bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Returns the value of directBufferPoolSize.
     * A performance tuning property. It specifies the pool size for the 'direct' byte buffers. These buffers
     * are used by XADisk for reading and writing files. Default value is 1000.
     * @return directBufferPoolSize
     */
    public Integer getDirectBufferPoolSize() {
        return directBufferPoolSize;
    }

    /**
     * Sets the value of directBufferPoolSize.
     * Sets directBufferPoolSize
     * A performance tuning property. It specifies the pool size for the 'direct' byte buffers. These buffers
     * are used by XADisk for reading and writing files. Default value is 1000.
     * @param directBufferPoolSize new value of directBufferPoolSize.
     */
    public void setDirectBufferPoolSize(Integer directBufferPoolSize) {
        this.directBufferPoolSize = directBufferPoolSize;
    }

    /**
     * Returns the value of nonDirectBufferPoolSize.
     * A performance tuning property. It specifies the pool size for the normal (non-direct) byte buffers. These buffers
     * are used by XADisk for reading and writing files, when there are no more pooled direct buffers. Default value is 1000.
     * @return nonDirectBufferPoolSize
     */
    public Integer getNonDirectBufferPoolSize() {
        return nonDirectBufferPoolSize;
    }

    /**
     * Sets the value of nonDirectBufferPoolSize.
     * A performance tuning property. It specifies the pool size for the normal (non-direct) byte buffers. These buffers
     * are used by XADisk for reading and writing files, when there are no more pooled direct buffers. Default value is 1000.
     * @param nonDirectBufferPoolSize new value of nonDirectBufferPoolSize.
     */
    public void setNonDirectBufferPoolSize(Integer nonDirectBufferPoolSize) {
        this.nonDirectBufferPoolSize = nonDirectBufferPoolSize;
    }

    /**
     * Returns the value of xaDiskHome.
     * This is also referred to as XADisk System Directory.
     * This is a directory where XADisk keeps all of its artifacts required for its functioning.
     * The directory that you specify may exist or not. If it exists, it must not be anything other than the one
     * you used for XADisk sometime earlier. If it does not exist, XADisk creates it during initialization.
     * You should not do any kind of modifications inside this directory; as it may lead to failure of XADisk.
     * One of the important things XADisk keeps here is the transaction logs.
     * @return xaDiskHome
     */
    public String getXaDiskHome() {
        return xaDiskHome;
    }

    /**
     * Sets the value of xaDiskHome.
     * This is also referred to as XADisk System Directory.
     * This is a directory where XADisk keeps all of its artifacts required for its functioning.
     * The directory that you specify may exist or not. If it exists, it must not be anything other than the one
     * you used for XADisk sometime earlier. If it does not exist, XADisk creates it during initialization.
     * You should not do any kind of modifications inside this directory; as it may lead to failure of XADisk.
     * One of the important things XADisk keeps here is the transaction logs.
     * @param xaDiskHome new value of xaDiskHome.
     */
    public void setXaDiskHome(String xaDiskHome) {
        this.xaDiskHome = xaDiskHome;
    }

    /**
     * Returns the value of transactionLogFileMaxSize.
     * This is the maximum size of a transaction log file (in bytes). XADisk maintains a transaction
     * log and rotates the current transaction log if its size exceeds this value. You should set this
     * value according to the maximum file-size allowed by your file-system. Default value is 1GB.
     * @return transactionLogFileMaxSize
     */
    public Long getTransactionLogFileMaxSize() {
        return transactionLogFileMaxSize;
    }

    /**
     * Sets the value of transactionLogFileMaxSize.
     * This is the maximum size of a transaction log file (in bytes). XADisk maintains a transaction
     * log and rotates the current transaction log if its size exceeds this value. You should set this
     * value according to the maximum file-size allowed by your file-system. Default value is 1GB.
     * @param transactionLogFileMaxSize new value of transactionLogFileMaxSize.
     */
    public void setTransactionLogFileMaxSize(Long transactionLogFileMaxSize) {
        this.transactionLogFileMaxSize = transactionLogFileMaxSize;
    }

    /**
     * Returns the value of cumulativeBufferSizeForDiskWrite.
     * A performance tuning property. XADisk doesn’t write its transaction logs one-by-one separately,
     * to the disk; it does so in big enough batches. This property mentions total size of transaction logs, in bytes,
     * when such a disk write takes place. Default value is 1000000 bytes.
     * @return cumulativeBufferSizeForDiskWrite
     */
    public Integer getCumulativeBufferSizeForDiskWrite() {
        return cumulativeBufferSizeForDiskWrite;
    }

    /**
     * Sets the value of cumulativeBufferSizeForDiskWrite.
     * A performance tuning property. XADisk doesn’t write its transaction logs one-by-one separately,
     * to the disk; it does so in big enough batches. This property mentions total size of transaction logs, in bytes,
     * when such a disk write takes place. Default value is 1000000 bytes.
     * @param cumulativeBufferSizeForDiskWrite new value of cumulativeBufferSizeForDiskWrite.
     */
    public void setCumulativeBufferSizeForDiskWrite(Integer cumulativeBufferSizeForDiskWrite) {
        this.cumulativeBufferSizeForDiskWrite = cumulativeBufferSizeForDiskWrite;
    }

    /**
     * Returns the value of directBufferIdleTime.
     * A performance tuning property. This is the number of seconds after which any pooled buffer
     * (whether direct or non-direct) is considered idle if not in use. An idle buffer is freed by a
     * background thread which runs periodically. The frequency of this is decided by a property called
     * bufferPoolRelieverInterval. Default values are 100 seconds.
     * @return directBufferIdleTime
     */
    public Integer getDirectBufferIdleTime() {
        return directBufferIdleTime;
    }

    /**
     * Returns the value of nonDirectBufferIdleTime.
     * A performance tuning property. This is the number of seconds after which any pooled buffer
     * (whether direct or non-direct) is considered idle if not in use. An idle buffer is freed by a
     * background thread which runs periodically. The frequency of this is decided by a property called
     * bufferPoolRelieverInterval. Default values are 100 seconds.
     * @return nonDirectBufferIdleTime
     */
    public Integer getNonDirectBufferIdleTime() {
        return nonDirectBufferIdleTime;
    }

    /**
     * Sets the value of directBufferIdleTime.
     * A performance tuning property. This is the number of seconds after which any pooled buffer
     * (whether direct or non-direct) is considered idle if not in use. An idle buffer is freed by a
     * background thread which runs periodically. The frequency of this is decided by a property called
     * bufferPoolRelieverInterval. Default values are 100 seconds.
     * @param directBufferIdleTime new value of directBufferIdleTime.
     */
    public void setDirectBufferIdleTime(Integer directBufferIdleTime) {
        this.directBufferIdleTime = directBufferIdleTime;
    }

    /**
     * Sets the value of nonDirectBufferIdleTime.
     * A performance tuning property. This is the number of seconds after which any pooled buffer
     * (whether direct or non-direct) is considered idle if not in use. An idle buffer is freed by a
     * background thread which runs periodically. The frequency of this is decided by a property called
     * bufferPoolRelieverInterval. Default values are 100 seconds.
     * @param nonDirectBufferIdleTime new value of nonDirectBufferIdleTime.
     */
    public void setNonDirectBufferIdleTime(Integer nonDirectBufferIdleTime) {
        this.nonDirectBufferIdleTime = nonDirectBufferIdleTime;
    }

    /**
     * Returns the value of bufferPoolRelieverInterval.
     * See the description for setDirectBufferIdleTime. Default value is 60 seconds.
     * @return bufferPoolRelieverInterval
     */
    public Integer getBufferPoolRelieverInterval() {
        return bufferPoolRelieverInterval;
    }

    /**
     * Sets the value of bufferPoolRelieverInterval.
     * See the description for setDirectBufferIdleTime. Default value is 60 seconds.
     * @param bufferPoolRelieverInterval new value of bufferPoolRelieverInterval.
     */
    public void setBufferPoolRelieverInterval(Integer bufferPoolRelieverInterval) {
        this.bufferPoolRelieverInterval = bufferPoolRelieverInterval;
    }

    /**
     * Returns the value of maxNonPooledBufferSize.
     * A performance tuning property. XADisk tries to hold its ongoing transactions' logs in memory
     * using byte buffers (these are not pooled). Such byte buffers are also used for normal file
     * reading and writing if the pooled buffers are all exhausted. As the total memory consumption
     * can significantly increase due to these buffers, this property is provided by XADisk to put
     * an upper limit on total size of in-memory buffers (non-pooled). Default value is 1000000.
     * @return maxNonPooledBufferSize
     */
    public Long getMaxNonPooledBufferSize() {
        return maxNonPooledBufferSize;
    }

    /**
     * Sets the value of maxNonPooledBufferSize.
     * A performance tuning property. XADisk tries to hold its ongoing transactions' logs in memory
     * using byte buffers (these are not pooled). Such byte buffers are also used for normal file
     * reading and writing if the pooled buffers are all exhausted. As the total memory consumption
     * can significantly increase due to these buffers, this property is provided by XADisk to put
     * an upper limit on total size of in-memory buffers (non-pooled). Default value is 1000000.
     * @param maxNonPooledBufferSize new value of maxNonPooledBufferSize.
     */
    public void setMaxNonPooledBufferSize(Long maxNonPooledBufferSize) {
        this.maxNonPooledBufferSize = maxNonPooledBufferSize;
    }

    /**
     * Returns the value of deadLockDetectorInterval.
     * This is the maximum size of a transaction log file (in bytes). XADisk maintains a transaction log and
     * rotates the current transaction log if its size exceeds this value. You should set this value according
     * to the maximum file-size allowed by your file-system. Default value is 1GB.
     * @return deadLockDetectorInterval
     */
    public Integer getDeadLockDetectorInterval() {
        return deadLockDetectorInterval;
    }

    /**
     * Sets the value of deadLockDetectorInterval.
     * This is the maximum size of a transaction log file (in bytes). XADisk maintains a transaction log and
     * rotates the current transaction log if its size exceeds this value. You should set this value according
     * to the maximum file-size allowed by your file-system. Default value is 1GB.
     * @param deadLockDetectorInterval new value of deadLockDetectorInterval.
     */
    public void setDeadLockDetectorInterval(Integer deadLockDetectorInterval) {
        this.deadLockDetectorInterval = deadLockDetectorInterval;
    }

    /**
     * Returns the value of lockTimeOut.
     * This is the time duration (in milliseconds) for which a request to acquire a
     * lock will wait if the lock is not immediately available. If the time exceeds
     * this value, an exception is thrown to the application to let it know. Default value is 10 seconds.
     * @return lockTimeOut
     */
    public Integer getLockTimeOut() {
        return lockTimeOut;
    }

    /**
     * Sets the value of lockTimeOut.
     * This is the time duration (in milliseconds) for which a request to acquire a
     * lock will wait if the lock is not immediately available. If the time exceeds
     * this value, an exception is thrown to the application to let it know. Default value is 10 seconds.
     * @param lockTimeOut new value of lockTimeOut.
     */
    public void setLockTimeOut(Integer lockTimeOut) {
        this.lockTimeOut = lockTimeOut;
    }

    /**
     * Returns the value of logLevel.
     * Logging is not yet implemented.
     * @return logLevel
     */
    public Integer getLogLevel() {
        return logLevel;
    }

    /**
     * Sets the value of logLevel.
     * Logging is not yet implemented.
     * @param logLevel new value of logLevel.
     */
    public void setLogLevel(Integer logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * Returns the value of maximumConcurrentEventDeliveries.
     * This is the upper bound on the number of Message Driven Beans concurrently processing the events from XADisk.
     * This property is applicable only for XADisk deployed in a JavaEE environment. Default value is 20.
     * @return maximumConcurrentEventDeliveries
     */
    public Integer getMaximumConcurrentEventDeliveries() {
        return maximumConcurrentEventDeliveries;
    }

    /**
     * Sets the value of maximumConcurrentEventDeliveries.
     * This is the upper bound on the number of Message Driven Beans concurrently processing the events from XADisk.
     * This property is applicable only for XADisk deployed in a JavaEE environment. Default value is 20.
     * @param maximumConcurrentEventDeliveries new value of maximumConcurrentEventDeliveries.
     */
    public void setMaximumConcurrentEventDeliveries(Integer maximumConcurrentEventDeliveries) {
        this.maximumConcurrentEventDeliveries = maximumConcurrentEventDeliveries;
    }

    /**
     * Returns the value of transactionTimeout.
     * This is maximum time (in seconds) for which any transaction in the system will be allowed to remain 'open'.
     * If a transaction times out, it will be rolled-back by the transaction timeout mechanism. Default value is 60 seconds.
     * @return transactionTimeout
     */
    public Integer getTransactionTimeout() {
        return transactionTimeout;
    }

    /**
     * Sets the value of transactionTimeout.
     * This is maximum time (in seconds) for which any transaction in the system will be allowed to remain 'open'.
     * If a transaction times out, it will be rolled-back by the transaction timeout mechanism. Default value is 60 seconds.
     * @param transactionTimeout new value of transactionTimeout.
     */
    public void setTransactionTimeout(Integer transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
    }

    /**
     * Returns the value of serverAddress.
     * Take a case when you have booted an XADisk instance xad1 and want to call it from five other applications.
     * If you had only one application, you can very well boot xad1 inside the same JVM as the application.
     * But with more application wanting to operate on the instance xad1, you would need to boot xad1 such that xad1
     * can also serve 'requests' coming from the other foreign applications. The two properties called ServerAddress and
     * ServerPort allow you to configure this XADisk instance. The port is the TCP port the XADisk instance will listen to.
     * The ServerAddress is also needed (you probably didn't expect it) and you can set it such that the address is reachable
     * from all the applications running on different JVMs. Note that this feature also allows applications on different
     * machines to work on the file-system on top of which an XADisk instance, xad1, is running. But if you don’t have
     * such 'remote' applications, you can simply set ServerAddress to 'localhost'; else set it to some address which must
     * be 'identifyable' and 'reachable' from all the remote applications. When XADisk is deployed in a JavaEE environment,
     * these address/port properties are also used to listen for inbound events from the remote XADisk instances. Default
     * values are '127.0.0.1' and 9999 respectively.
     * @return serverAddress
     */
    public String getServerAddress() {
        return serverAddress;
    }

    /**
     * Sets the value of serverAddress.
     * Take a case when you have booted an XADisk instance xad1 and want to call it from five other applications.
     * If you had only one application, you can very well boot xad1 inside the same JVM as the application.
     * But with more application wanting to operate on the instance xad1, you would need to boot xad1 such that xad1
     * can also serve 'requests' coming from the other foreign applications. The two properties called ServerAddress and
     * ServerPort allow you to configure this XADisk instance. The port is the TCP port the XADisk instance will listen to.
     * The ServerAddress is also needed (you probably didn't expect it) and you can set it such that the address is reachable
     * from all the applications running on different JVMs. Note that this feature also allows applications on different
     * machines to work on the file-system on top of which an XADisk instance, xad1, is running. But if you don’t have
     * such 'remote' applications, you can simply set ServerAddress to 'localhost'; else set it to some address which must
     * be 'identifyable' and 'reachable' from all the remote applications. When XADisk is deployed in a JavaEE environment,
     * these address/port properties are also used to listen for inbound events from the remote XADisk instances. Default
     * values are '127.0.0.1' and 9999 respectively.
     * @param serverAddress new value of serverAddress.
     */
    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    /**
     * Returns the value of serverPort.
     * See the description for getServerAddress.
     * @return serverPort
     */
    public Integer getServerPort() {
        return serverPort;
    }

    /**
     * Sets the value of serverPort.
     * See the description for setServerAddress.
     * @param serverPort new value of serverPort.
     */
    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }
}

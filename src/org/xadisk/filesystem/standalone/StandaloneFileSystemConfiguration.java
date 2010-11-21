package org.xadisk.filesystem.standalone;

import org.xadisk.filesystem.FileSystemConfiguration;

/**
 * This class represents the configuration object required for booting XADisk
 * from standalone Java programs. All of the three properties of this class are optional performance tuning properties.
 * When running XADisk from standalone Java programs, XADisk uses its own thin implementation of WorkManager
 * (which is otherwise available from the JavaEE Server). This WorkManager implementation relies on a JDK utility
 * class java.util.concurrent.ThreadPoolExecutor. The three tuning properties mentioned here are used as-is to set
 * the properties corePoolSize, maximumPoolSize and keepAliveTime of the ThreadPoolExecutor. Default values for these
 * properties are 10/Integer.MAX_VALUE/60 respectively.
 */

public class StandaloneFileSystemConfiguration extends FileSystemConfiguration {

    private int workManagerCorePoolSize = 10;
    private int workManagerMaxPoolSize = Integer.MAX_VALUE;
    private long workManagerKeepAliveTime = 60; //in seconds.

    /**
     * This constructor is called by Java applications to boot an XADisk instance in the same JVM.
     * @param xaDiskHome
     */
    public StandaloneFileSystemConfiguration(String xaDiskHome) {
        super(xaDiskHome);
    }

    /**
     * Returns the value of workManagerCorePoolSize.
     * Please see description of this class.
     * @return workManagerCorePoolSize
     */
    public int getWorkManagerCorePoolSize() {
        return workManagerCorePoolSize;
    }

    /**
     * Sets the value of workManagerCorePoolSize.
     * Please see description of this class.
     * @param workManagerCorePoolSize new value of workManagerCorePoolSize.
     */
    public void setWorkManagerCorePoolSize(int workManagerCorePoolSize) {
        this.workManagerCorePoolSize = workManagerCorePoolSize;
    }

    /**
     * Returns the value of workManagerMaxPoolSize.
     * Please see description of this class.
     * @return workManagerMaxPoolSize
     */
    public int getWorkManagerMaxPoolSize() {
        return workManagerMaxPoolSize;
    }

    /**
     * Sets the value of workManagerMaxPoolSize.
     * Please see description of this class.
     * @param workManagerMaxPoolSize new value of workManagerMaxPoolSize.
     */
    public void setWorkManagerMaxPoolSize(int workManagerMaxPoolSize) {
        this.workManagerMaxPoolSize = workManagerMaxPoolSize;
    }

    /**
     * Returns the value of workManagerKeepAliveTime.
     * Please see description of this class.
     * @return workManagerKeepAliveTime
     */
    public long getWorkManagerKeepAliveTime() {
        return workManagerKeepAliveTime;
    }

    /**
     * Sets the value of workManagerKeepAliveTime.
     * Please see description of this class.
     * @param workManagerKeepAliveTime new value of workManagerKeepAliveTime.
     */
    public void setWorkManagerKeepAliveTime(long workManagerKeepAliveTime) {
        this.workManagerKeepAliveTime = workManagerKeepAliveTime;
    }
}

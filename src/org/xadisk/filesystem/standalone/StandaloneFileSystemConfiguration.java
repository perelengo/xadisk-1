package org.xadisk.filesystem.standalone;

import org.xadisk.filesystem.FileSystemConfiguration;

/**
 * This class represents the configuration object required for booting XADisk
 * from standalone Java programs.
 * For details on all of these properties, please refer to the User Guide.
 */
public class StandaloneFileSystemConfiguration extends FileSystemConfiguration {

    private int workManagerCorePoolSize = 10;
    private int workManagerMaxPoolSize = Integer.MAX_VALUE;
    private long workManagerKeepAliveTime = 60; //in seconds.

    public StandaloneFileSystemConfiguration(String xaDiskHome) {
        super(xaDiskHome);
    }

    public int getWorkManagerCorePoolSize() {
        return workManagerCorePoolSize;
    }

    public void setWorkManagerCorePoolSize(int workManagerCorePoolSize) {
        this.workManagerCorePoolSize = workManagerCorePoolSize;
    }

    public int getWorkManagerMaxPoolSize() {
        return workManagerMaxPoolSize;
    }

    public void setWorkManagerMaxPoolSize(int workManagerMaxPoolSize) {
        this.workManagerMaxPoolSize = workManagerMaxPoolSize;
    }

    public long getWorkManagerKeepAliveTime() {
        return workManagerKeepAliveTime;
    }

    public void setWorkManagerKeepAliveTime(long workManagerKeepAliveTime) {
        this.workManagerKeepAliveTime = workManagerKeepAliveTime;
    }
}

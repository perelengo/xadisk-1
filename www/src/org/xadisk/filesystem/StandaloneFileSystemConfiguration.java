package org.xadisk.filesystem;

public class StandaloneFileSystemConfiguration extends FileSystemConfiguration {

    private int workManagerCorePoolSize = Integer.MAX_VALUE;
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

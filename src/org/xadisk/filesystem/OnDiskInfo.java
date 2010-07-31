package org.xadisk.filesystem;

public class OnDiskInfo {

    private final int logIndex;
    private final long location;

    public OnDiskInfo(int logIndex, long location) {
        this.logIndex=logIndex;
        this.location=location;
    }

    public int getLogIndex() {
        return logIndex;
    }

    public long getLocation() {
        return location;
    }
}

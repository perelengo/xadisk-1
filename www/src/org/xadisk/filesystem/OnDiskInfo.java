package org.xadisk.filesystem;

class OnDiskInfo {

    private final int logIndex;
    private final long location;

    OnDiskInfo(int logIndex, long location) {
        this.logIndex=logIndex;
        this.location=location;
    }

    int getLogIndex() {
        return logIndex;
    }

    long getLocation() {
        return location;
    }
}

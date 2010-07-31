package org.xadisk.filesystem;

import java.io.File;
import java.io.Serializable;

public class FileStateChangeEvent implements Serializable {

    public static final byte FILE_MODIFIED = 0x1;
    public static final byte FILE_DELETED = 0x2;
    public static final byte FILE_CREATED = 0x4;
    private final File file;
    private final boolean isDirectory;
    private final byte eventType;
    private transient final XidImpl enqueuingTransaction;

    FileStateChangeEvent(File file, boolean isDirectory, byte eventType, XidImpl enqueuingTransaction) {
        this.file = file;
        this.isDirectory = isDirectory;
        this.eventType = eventType;
        this.enqueuingTransaction = enqueuingTransaction;
    }

    public File getFile() {
        return file;
    }

    public byte getEventType() {
        return eventType;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    XidImpl getEnqueuingTransaction() {
        return enqueuingTransaction;
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FileStateChangeEvent) {
            FileStateChangeEvent event = (FileStateChangeEvent) obj;
            return event.file.equals(this.file) && event.eventType == this.eventType &&
                    event.isDirectory == this.isDirectory && event.enqueuingTransaction.equals(this.enqueuingTransaction);
        }
        return false;
    }
}

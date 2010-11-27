package org.xadisk.filesystem;

import java.io.File;
import java.io.Serializable;

/**
 * This class represents a file system event object passed to an MDB by XADisk
 * when an event, interesting to that MDB, takes place. Such MDBs are required to
 * implement the "onFileSystemEvent" method of interface "FileSystemEventListener".
 * This method has a single parameter object of type "FileStateChangeEvent".
 */
public class FileStateChangeEvent implements Serializable {

    private static final long serialVersionUID = 1L;
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

    /**
     * @return The file/directory on which this event has taken place.
     */
    public File getFile() {
        return file;
    }

    /**
     * @return The type of the event.
     */
    public byte getEventType() {
        return eventType;
    }

    /**
     * @return true, if the event has taken place on a directory.
     */
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
            FileStateChangeEvent that = (FileStateChangeEvent) obj;
            return that.file.equals(this.file) && that.eventType == this.eventType
                    && that.isDirectory == this.isDirectory
                    && that.enqueuingTransaction.equals(this.enqueuingTransaction);
        }
        return false;
    }

    @Override
    public String toString() {
        return "Event Type : " + this.eventType
                + " || File Name : " + this.file
                + " || Is a Directory : " + this.isDirectory;
    }
}

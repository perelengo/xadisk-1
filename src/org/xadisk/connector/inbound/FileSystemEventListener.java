package org.xadisk.connector.inbound;

import org.xadisk.filesystem.FileStateChangeEvent;

/**
 * An interface which must be implemented by the MDBs wanting
 * to receive file-system events from XADisk.
 */
public interface FileSystemEventListener {

    /**
     * This method is called by the JavaEE container when an event is published by XADisk
     * targeted for this MDB.
     * @param event The event object providing details of the event which has taken place.
     */
    public void onFileSystemEvent(FileStateChangeEvent event);
}

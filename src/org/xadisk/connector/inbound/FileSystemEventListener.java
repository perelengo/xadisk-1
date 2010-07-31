package org.xadisk.connector.inbound;

import org.xadisk.filesystem.FileStateChangeEvent;

public interface FileSystemEventListener {

    public void onFileSystemEvent(FileStateChangeEvent event);
}

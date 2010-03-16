package org.xadisk.connector;

import org.xadisk.filesystem.FileStateChangeEvent;

public interface FileSystemEventListener {

    public void onFileSystemEvent(FileStateChangeEvent event);
}

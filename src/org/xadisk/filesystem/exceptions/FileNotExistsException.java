package org.xadisk.filesystem.exceptions;

import java.io.File;

/**
 * This exception is thrown when an XADisk operation expects to find a file/directory
 * but it does not exist.
 */
public class FileNotExistsException extends XAApplicationException {
    
    public FileNotExistsException() {
    }
    
    public FileNotExistsException(File f) {
        super("The File with path " + f.getAbsolutePath() + " does not exist.");
    }
}

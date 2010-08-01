package org.xadisk.filesystem.exceptions;

import java.io.File;

public class FileNotExistsException extends XAApplicationException {
    
    public FileNotExistsException() {
    }
    
    public FileNotExistsException(File f) {
        super("The File with path " + f.getAbsolutePath() + " does not exist.");
    }
}

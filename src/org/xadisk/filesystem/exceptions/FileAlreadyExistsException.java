package org.xadisk.filesystem.exceptions;

/**
 * This exception is thrown when an XADisk operation wants to create a file/directory,
 * but the file/directory already exists.
 */
public class FileAlreadyExistsException extends XAApplicationException {
    public FileAlreadyExistsException() {
        super();
    }
}

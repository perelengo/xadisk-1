package org.xadisk.filesystem.exceptions;

/**
 * This exception is thrown when a directory delete operation was invoked
 * but the directory is not empty.
 */
public class DirectoryNotEmptyException extends XAApplicationException {

    public DirectoryNotEmptyException() {
        super();
    }

}

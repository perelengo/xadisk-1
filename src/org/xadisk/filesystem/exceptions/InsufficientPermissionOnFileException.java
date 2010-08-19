package org.xadisk.filesystem.exceptions;

/**
 * This exception is thrown when an operation cannot be performed due to
 * insufficient privileges over the file or directory.
 */
public class InsufficientPermissionOnFileException extends XAApplicationException {

    public InsufficientPermissionOnFileException() {
        super();
    }
}

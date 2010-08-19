package org.xadisk.filesystem.exceptions;

/**
 * This is the super class of all exceptions which are thrown when the underlying XADisk
 * system is all well, but the specific operation in question cannot be performed. These
 * exception are normal or mild and requires handling at the client application level.
 */
public class XAApplicationException extends Exception {

    public XAApplicationException() {
    }

    public XAApplicationException(String msg) {
        super(msg);
    }

    public XAApplicationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public XAApplicationException(Throwable cause) {
        super(cause);
    }
}

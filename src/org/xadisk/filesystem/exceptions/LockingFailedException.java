package org.xadisk.filesystem.exceptions;

/**
 * This exception is a super class of other exceptions which are thrown when a resource
 * locking fails.
 */
public class LockingFailedException extends XAApplicationException {
    public LockingFailedException() {
        super();
    }

    public LockingFailedException(Throwable cause) {
        super(cause);
    }
}

package org.xadisk.filesystem.exceptions;

public class LockingFailedException extends XAApplicationException {
    public LockingFailedException() {
        super();
    }

    public LockingFailedException(Throwable cause) {
        super(cause);
    }
}

package org.xadisk.filesystem.exceptions;

/**
 * This exception is thrown when a resource lock required for the operation
 * could not be acquired within the Lock time out interval.
 */
public class LockingTimedOutException extends LockingFailedException {

    public LockingTimedOutException() {
    }
}

package org.xadisk.filesystem.exceptions;

/**
 * This exception is thrown from a file system operation on XADisk when the
 * operation was waiting for a lock and was one of the responsible transactions
 * causing a deadlock and the current transaction was rolled back in
 * order to break the deadlock.
 */
public class DeadLockVictimizedException extends XAApplicationException {
    public DeadLockVictimizedException() {
        super();
    }
}

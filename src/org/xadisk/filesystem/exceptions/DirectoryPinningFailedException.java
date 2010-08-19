package org.xadisk.filesystem.exceptions;

/**
 * This exception is thrown when a directory MOVE operation cannot proceed
 * because the directory has already been locked by some other transaction and
 * hence could not be "pinned" for the MOVE operation. As of current implementation,
 * no wait is done if a directory cannot be pinned.
 */
public class DirectoryPinningFailedException extends LockingFailedException {

    public DirectoryPinningFailedException() {
        super();
    }
}

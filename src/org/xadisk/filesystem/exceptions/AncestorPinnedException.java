package org.xadisk.filesystem.exceptions;

/**
 * This exception is thrown when the requested operation cannot take place because
 * one of the ancestor directories of the file/directory (being operated on) has
 * been "pinned" (reserved/locked) by some other transaction. As of current implementation,
 * no wait is done if an ancestor directory is pinned.
 */
public class AncestorPinnedException extends LockingFailedException {

    public AncestorPinnedException() {
        super();
    }
}

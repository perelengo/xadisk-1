package org.xadisk.filesystem.exceptions;

/**
 * This exception is thrown to indicate that XADisk has not yet completed
 * its crash recovery after it was booted.
 */
public class RecoveryInProgressException extends XASystemException {

    public RecoveryInProgressException() {
        super();
    }
}

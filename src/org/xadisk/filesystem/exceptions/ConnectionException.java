package org.xadisk.filesystem.exceptions;

/**
 * This exception is thrown to indicate errors in communication to a remote XADisk
 * instance when some operation was being performed on the remote XADisk instance.
 */
public class ConnectionException extends XAApplicationException {

    public ConnectionException(Throwable cause) {
        super(cause);
    }
}

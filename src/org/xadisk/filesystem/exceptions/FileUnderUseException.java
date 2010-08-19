package org.xadisk.filesystem.exceptions;

/**
 * This exception is thrown to indicate that the requested operation cannot proceed
 * because a file is being read/written inside the current transaction, i.e. there is at least
 * one i/o stream opened for the file but not yet closed.
 */
public class FileUnderUseException extends XAApplicationException {

    public FileUnderUseException() {
        super();
    }

    public FileUnderUseException(String msg) {
        super(msg);
    }
}

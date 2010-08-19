package org.xadisk.filesystem.exceptions;

/**
 * This is the super class of all exceptions which are thrown because of a reason
 * associated to the whole XADisk system. For example, an exception thrown when
 * XADisk is still in crash recovery cycle, or when some serious issue
 * is seen during any kind of operation.
 */
public class XASystemException extends RuntimeException {

    public XASystemException(String msg) {
        super(msg);
    }

    public XASystemException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public XASystemException(Throwable cause) {
        super(cause);
    }

    public XASystemException() {
    }
}

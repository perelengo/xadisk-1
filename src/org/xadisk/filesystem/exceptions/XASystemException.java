package org.xadisk.filesystem.exceptions;

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

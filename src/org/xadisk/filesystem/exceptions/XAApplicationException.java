package org.xadisk.filesystem.exceptions;

public class XAApplicationException extends Exception {

    public XAApplicationException() {
    }
    
    public XAApplicationException(String msg) {
        super(msg);
    }

    public XAApplicationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public XAApplicationException(Throwable cause) {
        super(cause);
    }
}

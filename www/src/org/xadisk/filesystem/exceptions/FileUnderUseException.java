package org.xadisk.filesystem.exceptions;

public class FileUnderUseException extends XAApplicationException {

    public FileUnderUseException() {
        super();
    }

    public FileUnderUseException(String msg) {
        super(msg);
    }
}

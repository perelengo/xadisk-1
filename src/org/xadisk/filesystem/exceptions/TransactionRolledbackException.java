package org.xadisk.filesystem.exceptions;

public class TransactionRolledbackException extends XAApplicationException {

    public TransactionRolledbackException() {
        super();
    }

    public TransactionRolledbackException(Throwable cause) {
        super(cause);
    }
}

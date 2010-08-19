package org.xadisk.filesystem.exceptions;

/**
 * This exception is thrown when an operation cannot be performed because
 * the transaction which was once active with the Session/Connection has now
 * been rolled back.
 */
public class TransactionRolledbackException extends XAApplicationException {

    public TransactionRolledbackException() {
        super();
    }

    public TransactionRolledbackException(Throwable cause) {
        super(cause);
    }
}

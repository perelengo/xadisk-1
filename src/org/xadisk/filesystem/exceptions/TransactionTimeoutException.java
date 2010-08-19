package org.xadisk.filesystem.exceptions;

/**
 * This exception can appear as a "cause" of a TransactionRolledbackException
 * and indicates that the reason for the rollback was transaction time out.
 */
public class TransactionTimeoutException extends XAApplicationException {

    public TransactionTimeoutException() {
        super();
    }
}

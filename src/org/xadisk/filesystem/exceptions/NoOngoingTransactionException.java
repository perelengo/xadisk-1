package org.xadisk.filesystem.exceptions;

/**
 * This exception is thrown when an operation is invoked and an ongoing transaction
 * was required, but there is no ongoing transaction.
 */
public class NoOngoingTransactionException extends XAApplicationException {

    public NoOngoingTransactionException() {
        super();
    }
}

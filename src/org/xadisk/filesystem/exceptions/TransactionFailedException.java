package org.xadisk.filesystem.exceptions;

import org.xadisk.filesystem.TransactionInformation;

public class TransactionFailedException extends XAApplicationException {

    private static final long serialVersionUID = 1L;

    private byte[] transactionIdentifier;

    public TransactionFailedException(Throwable cause, TransactionInformation xid) {
        super(cause);
        this.transactionIdentifier = xid.getBytes();
    }

    public byte[] getTransactionIdentifier() {
        return transactionIdentifier;
    }
}

package org.xadisk.connector.outbound;

import javax.resource.spi.ConnectionEvent;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;

/**
 * This represents a transaction object which can be used by JavaEE applications
 * when they want to control the transaction on XADisk by themselves and in a resource-local
 * way (means, not using XA transaction).
 */
public class XADiskUserLocalTransaction {

    private final XADiskLocalTransaction localTxnImpl;
    private final XADiskManagedConnection mc;

    XADiskUserLocalTransaction(XADiskManagedConnection mc) {
        this.localTxnImpl = new XADiskLocalTransaction(mc);
        this.mc = mc;
    }

    /**
     * Starts a local transaction on the associated connection, and binds that
     * transaction to this object.
     */
    public void beginLocalTransaction() {
        localTxnImpl._begin();
        mc.raiseUserLocalTransactionEvent(ConnectionEvent.LOCAL_TRANSACTION_STARTED);
    }

    /**
     * Commits the local transaction bound to this object.
     * @throws TransactionRolledbackException
     */
    public void commitLocalTransaction() throws TransactionRolledbackException {
        localTxnImpl._commit();
        mc.raiseUserLocalTransactionEvent(ConnectionEvent.LOCAL_TRANSACTION_COMMITTED);
    }

    /**
     * Rolls back the local transaction bound to this object.
     * @throws TransactionRolledbackException
     */
    public void rollbackLocalTransaction() throws TransactionRolledbackException {
        localTxnImpl._rollback();
        mc.raiseUserLocalTransactionEvent(ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK);
    }

    /**
     * Gets the transaction timeout value for the current transaction.
     * @return the transaction timeout value, in seconds.
     */
    public int getTransactionTimeOut() {
        return localTxnImpl.getTransactionTimeOut();
    }

    /**
     * Sets the transaction timeout value for the current transaction.
     * @param transactionTimeOut
     */
    public void setTransactionTimeOut(int transactionTimeOut) {
        localTxnImpl.setTransactionTimeOut(transactionTimeOut);
    }
}

package org.xadisk.bridge.proxies.interfaces;

import org.xadisk.filesystem.exceptions.TransactionRolledbackException;

/**
 * This interface is used by standalone Java applications to invoke i/o operations
 * on XADisk and also to control the ongoing transaction.
 */
public interface Session extends XADiskBasicIOOperations {

    /**
     * Sets the transaction timeout value for the transaction associated with this Session.
     * @param transactionTimeout
     * @return true, if the operation succeeds.
     */
    public boolean setTransactionTimeout(int transactionTimeout);

    /**
     * Returns the current transaction timeout value.
     * @return the transaction timeout value.
     */
    public int getTransactionTimeout();

    /**
     * Rolls back the transaction associated with this Session.
     * @throws TransactionRolledbackException
     */
    public void rollback() throws TransactionRolledbackException;

    /**
     * Commits the transaction associated with this Session.
     * @throws TransactionRolledbackException
     */
    public void commit() throws TransactionRolledbackException;

    /**
     * Only for internal use.
     * @param onePhase
     * @throws TransactionRolledbackException
     */
    public void commit(boolean onePhase) throws TransactionRolledbackException;

    /**
     * Only for internal use.
     * @throws TransactionRolledbackException
     */
    public void prepare() throws TransactionRolledbackException;
}

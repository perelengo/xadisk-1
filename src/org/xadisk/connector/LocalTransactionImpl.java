package org.xadisk.connector;

import javax.resource.ResourceException;
import javax.resource.spi.LocalTransaction;
import org.xadisk.filesystem.XAFileSystem;
import org.xadisk.filesystem.exceptions.TransactionAlreadyAssociatedException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;

public class LocalTransactionImpl implements LocalTransaction {

    private final ManagedConnectionImpl mc;
    private int transactionTimeOut;

    public LocalTransactionImpl(ManagedConnectionImpl mc) {
        this.mc = mc;
        this.transactionTimeOut = XAFileSystem.getXAFileSystem().getDefaultTransactionTimeout();
    }

    void _begin() throws TransactionAlreadyAssociatedException {
        mc.setTypeOfOngoingTransaction(ManagedConnectionImpl.LOCAL_TRANSACTION);
        mc.refreshSessionForBeginLocalTransaction().setTransactionTimeout(transactionTimeOut);
    }

    @Override
    public void begin() throws ResourceException {
        try {
            _begin();
        } catch (TransactionAlreadyAssociatedException taae) {
            throw new ResourceException(taae);
        }
    }

    void _rollback() throws TransactionRolledbackException {
        mc.setTypeOfOngoingTransaction(ManagedConnectionImpl.NO_TRANSACTION);
        mc.getSessionOfLocalTransaction().rollback();
    }

    @Override
    public void rollback() throws ResourceException {
        try {
            _rollback();
        } catch (TransactionRolledbackException taae) {
            throw new ResourceException(taae);
        }
    }

    void _commit() throws TransactionRolledbackException {
        mc.setTypeOfOngoingTransaction(ManagedConnectionImpl.NO_TRANSACTION);
        mc.getSessionOfLocalTransaction().commit(true);
    }

    @Override
    public void commit() throws ResourceException {
        try {
            _commit();
        } catch (TransactionRolledbackException taae) {
            throw new ResourceException(taae);
        }
    }

    int getTransactionTimeOut() {
        return transactionTimeOut;
    }

    void setTransactionTimeOut(int transactionTimeOut) {
        this.transactionTimeOut = transactionTimeOut;
    }
}

package org.xadisk.connector.outbound;

import javax.resource.spi.ConnectionEvent;
import org.xadisk.filesystem.exceptions.TransactionAlreadyAssociatedException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;

public class UserLocalTransaction {

    private final LocalTransactionImpl localTxnImpl;
    private final ManagedConnectionImpl mc;

    UserLocalTransaction(ManagedConnectionImpl mc) {
        this.localTxnImpl = new LocalTransactionImpl(mc);
        this.mc = mc;
    }

    public void beginLocalTransaction() throws TransactionAlreadyAssociatedException {
        localTxnImpl._begin();
        mc.raiseUserLocalTransactionEvent(ConnectionEvent.LOCAL_TRANSACTION_STARTED);
    }

    public void commitLocalTransaction() throws TransactionRolledbackException {
        localTxnImpl._commit();
        mc.raiseUserLocalTransactionEvent(ConnectionEvent.LOCAL_TRANSACTION_COMMITTED);
    }

    public void rollbackLocalTransaction() throws TransactionRolledbackException {
        localTxnImpl._rollback();
        mc.raiseUserLocalTransactionEvent(ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK);
    }

    public int getTransactionTimeOut() {
        return localTxnImpl.getTransactionTimeOut();
    }

    public void setTransactionTimeOut(int transactionTimeOut) {
        localTxnImpl.setTransactionTimeOut(transactionTimeOut);
    }
}

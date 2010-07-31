package org.xadisk.connector.outbound;

import java.io.IOException;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.Session;
import java.io.PrintWriter;
import java.util.HashSet;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import org.xadisk.connector.XAResourceImpl;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.XidImpl;
import org.xadisk.filesystem.exceptions.NoOngoingTransactionException;
import org.xadisk.bridge.proxies.impl.RemoteXAFileSystem;

public class ManagedConnectionImpl implements ManagedConnection {

    private final HashSet<ConnectionEventListener> listeners = new HashSet<ConnectionEventListener>(10);
    private volatile PrintWriter logWriter;
    private final HashSet<ConnectionHandle> connectionHandles = new HashSet<ConnectionHandle>(2);
    private volatile Session sessionOfXATransaction;
    private volatile Session sessionOfLocalTransaction;
    private volatile byte typeOfOngoingTransaction;
    private boolean publishFileStateChangeEventsOnCommit = false;
    private final XAFileSystem theXAFileSystem;
    private final XAResourceImpl xaResourceImpl;
    private final LocalTransactionImpl localTransactionImpl;
    private volatile boolean memorySynchTrigger = false;
    public static final byte NO_TRANSACTION = 0;
    public static final byte LOCAL_TRANSACTION = 1;
    public static final byte XA_TRANSACTION = 2;

    public ManagedConnectionImpl() {
        this.theXAFileSystem = NativeXAFileSystem.getXAFileSystem();
        this.xaResourceImpl = new XAResourceImpl(this);
        this.localTransactionImpl = new LocalTransactionImpl(this);
    }

    public ManagedConnectionImpl(String serverAddress, Integer serverPort) throws IOException {
        this.theXAFileSystem = new RemoteXAFileSystem(serverAddress, serverPort);
        this.xaResourceImpl = new XAResourceImpl(this);
        this.localTransactionImpl = new LocalTransactionImpl(this);
    }

    public XAFileSystem getTheUnderlyingXAFileSystem() {
        return theXAFileSystem;
    }

    public void associateConnection(Object connection) throws ResourceException {
        if (!(connection instanceof ConnectionHandle)) {
            throw new ResourceException("Unexpected type for connection handle.");
        }
        ((ConnectionHandle) connection).setManagedConnection(this);
        invalidateCache();
        connectionHandles.add((ConnectionHandle) connection);
        flushCacheToMainMemory();
    }

    public void cleanup() throws ResourceException {
    }

    public void destroy() throws ResourceException {
    }

    public Object getConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        ConnectionHandle temp = new ConnectionHandle(this);
        invalidateCache();
        connectionHandles.add(temp);
        flushCacheToMainMemory();
        return temp;
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        return localTransactionImpl;
    }

    public XAResource getXAResource() throws ResourceException {
        return xaResourceImpl;
    }

    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new ManagedConnectionMetaData() {

            public String getEISProductName() throws ResourceException {
                return "XADiskFileSystem";
            }

            public String getEISProductVersion() throws ResourceException {
                return "1.0";
            }

            public int getMaxConnections() throws ResourceException {
                return 0;
            }

            public String getUserName() throws ResourceException {
                return "irrelevant";
            }
        };
    }

    public void addConnectionEventListener(ConnectionEventListener cel) {
        invalidateCache();
        listeners.add(cel);
        flushCacheToMainMemory();
    }

    public void removeConnectionEventListener(ConnectionEventListener cel) {
        invalidateCache();
        listeners.remove(cel);
        flushCacheToMainMemory();
    }

    public PrintWriter getLogWriter() throws ResourceException {
        return this.logWriter;
    }

    public void setLogWriter(PrintWriter logWriter) throws ResourceException {
        this.logWriter = logWriter;
    }

    void connectionClosed(ConnectionHandle connection) {
        connectionHandles.remove(connection);
        raiseConnectionEvent(new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED));
    }

    private void raiseConnectionEvent(ConnectionEvent ce) {
        invalidateCache();
        for (ConnectionEventListener cel : listeners) {
            switch (ce.getId()) {
                case ConnectionEvent.CONNECTION_CLOSED:
                    cel.connectionClosed(ce);
                    break;
                case ConnectionEvent.CONNECTION_ERROR_OCCURRED:
                    cel.connectionErrorOccurred(ce);
                    break;
                case ConnectionEvent.LOCAL_TRANSACTION_STARTED:
                    cel.localTransactionStarted(ce);
                    break;
                case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED:
                    cel.localTransactionCommitted(ce);
                    break;
                case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK:
                    cel.localTransactionRolledback(ce);
                    break;
            }
        }
    }

    void raiseUserLocalTransactionEvent(int transactionalEvent) {
        ConnectionEvent ce = new ConnectionEvent(this, transactionalEvent);
        raiseConnectionEvent(ce);
    }

    Session getSessionOfXATransaction() {
        return this.sessionOfXATransaction;
    }

    Session getSessionOfLocalTransaction() {
        return sessionOfLocalTransaction;
    }

    public void setSessionOfExistingXATransaction(Session session) {
        this.sessionOfXATransaction = session;
    }

    public Session refreshSessionForNewXATransaction(XidImpl xid) {
        this.sessionOfXATransaction = theXAFileSystem.createSessionForXATransaction(xid);
        this.sessionOfXATransaction.setPublishFileStateChangeEventsOnCommit(publishFileStateChangeEventsOnCommit);
        return this.sessionOfXATransaction;
    }

    public Session refreshSessionForBeginLocalTransaction() {
        this.sessionOfLocalTransaction = theXAFileSystem.createSessionForLocalTransaction();
        this.sessionOfLocalTransaction.setPublishFileStateChangeEventsOnCommit(publishFileStateChangeEventsOnCommit);
        return this.sessionOfLocalTransaction;
    }

    public void setTypeOfOngoingTransaction(byte typeOfOngoingTransaction) {
        this.typeOfOngoingTransaction = typeOfOngoingTransaction;
    }

    Session getSessionForCurrentWorkAssociation() throws NoOngoingTransactionException {
        switch (typeOfOngoingTransaction) {
            case LOCAL_TRANSACTION:
                return sessionOfLocalTransaction;
            case XA_TRANSACTION:
                return sessionOfXATransaction;
        }
        throw new NoOngoingTransactionException();
    }

    private void flushCacheToMainMemory() {
        memorySynchTrigger = true;
    }

    private void invalidateCache() {
        boolean temp = memorySynchTrigger;
    }

    boolean isPublishFileStateChangeEventsOnCommit() {
        return publishFileStateChangeEventsOnCommit;
    }

    void setPublishFileStateChangeEventsOnCommit(boolean publishFileStateChangeEventsOnCommit) {
        this.publishFileStateChangeEventsOnCommit = publishFileStateChangeEventsOnCommit;
    }
}

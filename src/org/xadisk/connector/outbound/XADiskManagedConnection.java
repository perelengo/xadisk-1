/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.connector.outbound;

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
import org.xadisk.bridge.proxies.impl.XADiskRemoteManagedConnection;
import org.xadisk.connector.XAResourceImpl;
import org.xadisk.filesystem.XAFileSystemCommonness;
import org.xadisk.filesystem.XidImpl;
import org.xadisk.filesystem.exceptions.NoTransactionAssociatedException;

public class XADiskManagedConnection implements ManagedConnection {

    private final HashSet<ConnectionEventListener> listeners = new HashSet<ConnectionEventListener>(10);
    private volatile PrintWriter logWriter;
    private final HashSet<XADiskConnection> connectionHandles = new HashSet<XADiskConnection>(2);
    private volatile Session sessionOfXATransaction;
    private volatile Session sessionOfLocalTransaction;
    private volatile byte typeOfCurrentTransaction = NO_TRANSACTION;
    private boolean publishFileStateChangeEventsOnCommit = false;
    private long fileLockWaitTimeout = 100;
    private final String instanceId;
    protected volatile XAFileSystem theXAFileSystem;
    protected volatile XAResourceImpl xaResourceImpl;
    private volatile XADiskLocalTransaction localTransactionImpl;
    private volatile boolean memorySynchTrigger = false;
    public static final byte NO_TRANSACTION = 0;
    public static final byte LOCAL_TRANSACTION = 1;
    public static final byte XA_TRANSACTION = 2;

    public XADiskManagedConnection(XAFileSystem xaFileSystem, String instanceId) {
        this.theXAFileSystem = xaFileSystem;
        this.xaResourceImpl = new XAResourceImpl(this);
        this.localTransactionImpl = new XADiskLocalTransaction(this);
        this.instanceId = instanceId;
    }

    public XAFileSystem getTheUnderlyingXAFileSystem() {
        return theXAFileSystem;
    }

    public void associateConnection(Object connection) throws ResourceException {
        if (!(connection instanceof XADiskConnection)) {
            throw new ResourceException("Unexpected type for connection handle.");
        }
        ((XADiskConnectionImpl) connection).setManagedConnection(this);
        invalidateCache();
        connectionHandles.add((XADiskConnection) connection);
        flushCacheToMainMemory();
    }

    public void cleanup() throws ResourceException {
        this.xaResourceImpl = new XAResourceImpl(this);
        this.localTransactionImpl = new XADiskLocalTransaction(this);
        this.connectionHandles.clear();
        //DO NOT clear the listeners. When trying to implement pooling, this guy
        //tested my patience. [this.listeners.clear();]
        this.publishFileStateChangeEventsOnCommit = false;
        this.fileLockWaitTimeout = 100;
        this.sessionOfLocalTransaction = null;
        this.sessionOfXATransaction = null;
        this.typeOfCurrentTransaction = NO_TRANSACTION;
    }

    public void destroy() throws ResourceException {
    }

    public Object getConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        XADiskConnection temp = new XADiskConnectionImpl(this);
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
        return new XADiskManagedConnectionMetaData();
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

    void connectionClosed(XADiskConnection connection) {
        connectionHandles.remove(connection);
        ConnectionEvent connectionEvent = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        connectionEvent.setConnectionHandle(connection);
        raiseConnectionEvent(connectionEvent);
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
        this.sessionOfXATransaction = ((XAFileSystemCommonness)theXAFileSystem).createSessionForXATransaction(xid);
        this.sessionOfXATransaction.setPublishFileStateChangeEventsOnCommit(publishFileStateChangeEventsOnCommit);
        this.sessionOfXATransaction.setFileLockWaitTimeout(fileLockWaitTimeout);
        return this.sessionOfXATransaction;
    }

    public Session refreshSessionForBeginLocalTransaction() {
        this.sessionOfLocalTransaction = theXAFileSystem.createSessionForLocalTransaction();
        this.sessionOfLocalTransaction.setPublishFileStateChangeEventsOnCommit(publishFileStateChangeEventsOnCommit);
        this.sessionOfLocalTransaction.setFileLockWaitTimeout(fileLockWaitTimeout);
        return this.sessionOfLocalTransaction;
    }

    public void setTypeOfCurrentTransaction(byte typeOfCurrentTransaction) {
        this.typeOfCurrentTransaction = typeOfCurrentTransaction;
    }

    Session getSessionForCurrentWorkAssociation() throws NoTransactionAssociatedException {
        switch (typeOfCurrentTransaction) {
            case LOCAL_TRANSACTION:
                return sessionOfLocalTransaction;
            case XA_TRANSACTION:
                return sessionOfXATransaction;
        }
        throw new NoTransactionAssociatedException();
    }

    private void flushCacheToMainMemory() {
        memorySynchTrigger = true;
    }

    private void invalidateCache() {
        boolean temp = memorySynchTrigger;
    }

    void setPublishFileStateChangeEventsOnCommit(boolean publishFileStateChangeEventsOnCommit) {
        this.publishFileStateChangeEventsOnCommit = publishFileStateChangeEventsOnCommit;
        switch (typeOfCurrentTransaction) {
            case LOCAL_TRANSACTION:
                sessionOfLocalTransaction.setPublishFileStateChangeEventsOnCommit(
                        publishFileStateChangeEventsOnCommit);
                break;
            case XA_TRANSACTION:
                sessionOfXATransaction.setPublishFileStateChangeEventsOnCommit(
                        publishFileStateChangeEventsOnCommit);
                break;
        }
    }

    public void setFileLockWaitTimeout(long fileLockWaitTimeout) {
        this.fileLockWaitTimeout = fileLockWaitTimeout;
        switch (typeOfCurrentTransaction) {
            case LOCAL_TRANSACTION:
                sessionOfLocalTransaction.setFileLockWaitTimeout(fileLockWaitTimeout);
                break;
            case XA_TRANSACTION:
                sessionOfXATransaction.setFileLockWaitTimeout(fileLockWaitTimeout);
                break;
        }
    }

    public boolean pointsToSameXADisk(XADiskManagedConnection thatMC) {
        if (thatMC instanceof XADiskManagedConnection
                && !(thatMC instanceof XADiskRemoteManagedConnection)) {
            return this.instanceId.equals(thatMC.instanceId);
        } else {
            return false;
        }
    }
}

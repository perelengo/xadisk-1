package org.xadisk.connector.outbound;

import org.xadisk.bridge.proxies.interfaces.XADiskBasicIOOperations;

/**
 * This class represents the connection object used inside JavaEE applications
 * for calling I/O operations on XADisk.
 */
public interface XADiskConnection extends XADiskBasicIOOperations {

    /**
     * Returns an instance of local transaction object which can be used by JavaEE applications
     * if they want to use only a local transaction (not XA) on this connection.
     * @return a transaction object for demarcation of local transactions on this connection.
     */
    public XADiskUserLocalTransaction getUserLocalTransaction();

    /**
     * Closes this connection. This connection object can't be used after closing it.
     */
    public void close();
}

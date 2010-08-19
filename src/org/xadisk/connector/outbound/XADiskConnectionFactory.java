package org.xadisk.connector.outbound;

import java.io.Serializable;
import javax.resource.Referenceable;
import javax.resource.ResourceException;

/**
 * This class represents a connection factory used by JavaEE applications to obtain
 * connections to XADisk.
 */
public interface XADiskConnectionFactory extends Serializable, Referenceable {

    /**
     * Retrieves a new connection handle to interact with XADisk.
     * @return a new connection object.
     * @throws ResourceException
     */
    public XADiskConnection getConnection() throws ResourceException;
}

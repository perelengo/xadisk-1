package org.xadisk.connector.outbound;

import java.io.Serializable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;

public class ConnectionManagerImpl implements ConnectionManager, Serializable {

    public Object allocateConnection(ManagedConnectionFactory arg0, ConnectionRequestInfo arg1) throws ResourceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}

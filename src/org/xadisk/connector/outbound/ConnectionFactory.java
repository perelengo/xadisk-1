package org.xadisk.connector.outbound;

import java.io.Serializable;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

public class ConnectionFactory implements Serializable, Referenceable {

    private final ManagedConnectionFactory mcf;
    private final ConnectionManager cm;

    private Reference ref;

    protected ConnectionFactory(ManagedConnectionFactory mcf, ConnectionManager cm)
    {
        this.mcf=mcf;
        this.cm=cm;
    }

    public ConnectionHandle getConnection() throws ResourceException
    {
        return (ConnectionHandle)cm.allocateConnection(mcf, null);
    }

    public Reference getReference() throws NamingException {
        return ref;
    }

    public void setReference(Reference ref) {
        this.ref=ref;
    }
}

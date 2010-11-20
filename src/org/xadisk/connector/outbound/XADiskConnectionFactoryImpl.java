package org.xadisk.connector.outbound;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import org.xadisk.bridge.proxies.interfaces.XADiskRemoteConnectionFactory;

public class XADiskConnectionFactoryImpl implements XADiskConnectionFactory, XADiskRemoteConnectionFactory {

    private final XADiskManagedConnectionFactory mcf;
    private final ConnectionManager cm;
    private Reference ref;

    public XADiskConnectionFactoryImpl(XADiskManagedConnectionFactory mcf, ConnectionManager cm) {
        this.mcf = mcf;
        this.cm = cm;
    }

    public XADiskConnection getConnection() throws ResourceException {
        return (XADiskConnection) cm.allocateConnection(mcf, null);
    }

    public Reference getReference() throws NamingException {
        return ref;
    }

    public void setReference(Reference ref) {
        this.ref = ref;
    }
}

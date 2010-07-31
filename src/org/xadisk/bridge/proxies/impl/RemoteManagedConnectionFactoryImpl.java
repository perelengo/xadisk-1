package org.xadisk.bridge.proxies.impl;

import java.io.IOException;
import java.util.Set;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.security.auth.Subject;
import org.xadisk.connector.outbound.ManagedConnectionFactoryImpl;
import org.xadisk.connector.outbound.ManagedConnectionImpl;

public class RemoteManagedConnectionFactoryImpl extends ManagedConnectionFactoryImpl {

    private String serverAddress;
    private Integer serverPort;

    public RemoteManagedConnectionFactoryImpl() {
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        try {
            return new ManagedConnectionImpl(serverAddress, serverPort);
        } catch(IOException ioe) {
            throw new ResourceException(ioe);
        }
    }

    @Override
    public ManagedConnection matchManagedConnections(Set candidates, Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        //we don't keep any info in CRI, so just returning first one is good.
        return (ManagedConnection)candidates.iterator().next();
    }

    @Override
    public int hashCode() {
        return 102;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RemoteManagedConnectionFactoryImpl) {
            RemoteManagedConnectionFactoryImpl mcf = (RemoteManagedConnectionFactoryImpl) obj;
            return (mcf.serverAddress == null ? this.serverAddress == null : mcf.serverAddress.equals(this.serverAddress)) && mcf.serverPort == this.serverPort;
        }
        return false;
    }
}

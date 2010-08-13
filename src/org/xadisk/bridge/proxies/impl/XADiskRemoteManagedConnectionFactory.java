package org.xadisk.bridge.proxies.impl;

import java.io.IOException;
import java.util.Set;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.security.auth.Subject;
import org.xadisk.connector.outbound.XADiskConnectionFactoryImpl;
import org.xadisk.connector.outbound.XADiskManagedConnectionFactory;

public class XADiskRemoteManagedConnectionFactory extends XADiskManagedConnectionFactory {

    private String serverAddress;
    private Integer serverPort;

    public XADiskRemoteManagedConnectionFactory() {
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
            return new XADiskRemoteManagedConnection(serverAddress, serverPort);
        } catch (IOException ioe) {
            throw new ResourceException(ioe);
        }
    }

    @Override
    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new XADiskConnectionFactoryImpl(this, cm);
    }

    @Override
    public int hashCode() {
        return serverAddress.hashCode() + serverPort.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof XADiskRemoteManagedConnectionFactory) {
            XADiskRemoteManagedConnectionFactory mcf = (XADiskRemoteManagedConnectionFactory) obj;
            return (mcf.serverAddress == null ? this.serverAddress == null : mcf.serverAddress.equals(this.serverAddress)) && mcf.serverPort == this.serverPort;
        }
        return false;
    }
}

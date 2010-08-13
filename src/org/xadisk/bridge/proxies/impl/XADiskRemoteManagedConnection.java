package org.xadisk.bridge.proxies.impl;

import java.io.IOException;
import javax.resource.ResourceException;
import org.xadisk.connector.outbound.XADiskManagedConnection;

public class XADiskRemoteManagedConnection extends XADiskManagedConnection {

    private final String serverAddress;
    private final Integer serverPort;

    public XADiskRemoteManagedConnection(String serverAddress, Integer serverPort) throws IOException {
        super(new RemoteXAFileSystem(serverAddress, serverPort));
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    @Override
    public void cleanup() throws ResourceException {
        super.cleanup();
        ((RemoteXAFileSystem) theXAFileSystem).shutdown();
        //we shouldn't switch the xaFS object because the container might still
        //be using the "older" xaResource object which further points to the older
        //xaFS object. So, we rather keep using the "older" "xaFS" and don't reset it.
        //super.theXAFileSystem = new RemoteXAFileSystem(serverAddress, serverPort);
    }

    @Override
    public void destroy() throws ResourceException {
        super.destroy();
        ((RemoteXAFileSystem) theXAFileSystem).shutdown();
    }

    public boolean pointsToSameRemoteXADisk(XADiskRemoteManagedConnection thatMC) {
        return this.serverAddress.equalsIgnoreCase(thatMC.serverAddress)
                && this.serverPort.equals(thatMC.serverPort);
    }
}

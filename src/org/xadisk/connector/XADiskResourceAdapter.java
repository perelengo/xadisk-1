package org.xadisk.connector;

import org.xadisk.connector.inbound.LocalEventProcessingXAResource;
import org.xadisk.connector.inbound.EndPointActivation;
import org.xadisk.connector.inbound.XADiskActivationSpecImpl;
import java.io.IOException;
import java.io.Serializable;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import org.xadisk.filesystem.FileSystemConfiguration;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.exceptions.XASystemException;

public class XADiskResourceAdapter extends FileSystemConfiguration implements ResourceAdapter, Serializable {

    private NativeXAFileSystem xaFileSystem;

    public void start(BootstrapContext bsContext) throws ResourceAdapterInternalException {
        try {
            NativeXAFileSystem.bootXAFileSystem(this, bsContext.getWorkManager());
            this.xaFileSystem = NativeXAFileSystem.getXAFileSystem();
        } catch (XASystemException xase) {
            throw new ResourceAdapterInternalException(xase);
        }
    }

    public void stop() {
        try {
            xaFileSystem.shutdown();
        } catch (IOException ioe) {
        }
    }

    public void endpointActivation(MessageEndpointFactory mef, ActivationSpec as) throws ResourceException {
        EndPointActivation epActivation = new EndPointActivation(mef, (XADiskActivationSpecImpl) as);
        xaFileSystem.registerEndPointActivation(epActivation);
    }

    public void endpointDeactivation(MessageEndpointFactory mef, ActivationSpec as) {
        EndPointActivation epActivation = new EndPointActivation(mef, (XADiskActivationSpecImpl) as);
        xaFileSystem.deRegisterEndPointActivation(epActivation);
    }

    public XAResource[] getXAResources(ActivationSpec[] as) throws ResourceException {
        LocalEventProcessingXAResource xar[] = new LocalEventProcessingXAResource[1];
        xar[0] = new LocalEventProcessingXAResource(xaFileSystem);
        return xar;
    }
}

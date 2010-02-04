package org.xadisk.connector;

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
import org.xadisk.filesystem.XAFileSystem;
import org.xadisk.filesystem.exceptions.XASystemException;

public class XADiskResourceAdapter extends FileSystemConfiguration implements ResourceAdapter, Serializable {

    private XAFileSystem xaFileSystem;

    @Override
    public void start(BootstrapContext bsContext) throws ResourceAdapterInternalException {
        try {
            XAFileSystem.bootXAFileSystem(this, bsContext.getWorkManager());
            this.xaFileSystem = XAFileSystem.getXAFileSystem();
        } catch (XASystemException xase) {
            throw new ResourceAdapterInternalException(xase);
        }
    }

    @Override
    public void stop() {
        try {
            xaFileSystem.shutdown();
        } catch(IOException ioe) {
        }
    }

    @Override
    public void endpointActivation(MessageEndpointFactory mef, ActivationSpec as) throws ResourceException {
        EndPointActivation epActivation = new EndPointActivation(mef, (ActivationSpecImpl) as);
        xaFileSystem.getFileSystemEventDelegator().registerActivation(epActivation);
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory mef, ActivationSpec as) {
        EndPointActivation epActivation = new EndPointActivation(mef, (ActivationSpecImpl) as);
        xaFileSystem.getFileSystemEventDelegator().deRegisterActivation(epActivation);
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] as) throws ResourceException {
        EventProcessingXAResource xar[] = new EventProcessingXAResource[1];
        xar[0] = new EventProcessingXAResource(xaFileSystem);
        return xar;
    }
}

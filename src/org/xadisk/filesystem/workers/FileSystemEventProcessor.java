package org.xadisk.filesystem.workers;

import java.lang.reflect.Method;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import org.xadisk.bridge.proxies.impl.RemoteMessageEndpoint;
import org.xadisk.connector.inbound.LocalEventProcessingXAResource;
import org.xadisk.connector.inbound.FileSystemEventListener;
import org.xadisk.filesystem.FileStateChangeEvent;
import org.xadisk.filesystem.NativeXAFileSystem;

public class FileSystemEventProcessor implements Work {

    private final MessageEndpointFactory mef;
    private final FileStateChangeEvent event;
    private final NativeXAFileSystem xaFileSystem;

    FileSystemEventProcessor(MessageEndpointFactory mef, FileStateChangeEvent event, NativeXAFileSystem xaFileSystem) {
        this.mef = mef;
        this.event = event;
        this.xaFileSystem = xaFileSystem;
    }

    public void release() {
    }

    public void run() {
        LocalEventProcessingXAResource epXAR = new LocalEventProcessingXAResource(xaFileSystem, event);
        MessageEndpoint mep = null;
        while (mep == null) {
            try {
                mep = mef.createEndpoint(epXAR);
            } catch (UnavailableException uae) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        try {
            Method methodToInvoke = FileSystemEventListener.class.getMethod("onFileSystemEvent",
                    FileStateChangeEvent.class);
            mep.beforeDelivery(methodToInvoke);
            methodToInvoke.invoke(mep, event);
            mep.afterDelivery();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mep.release();
        if(mep instanceof RemoteMessageEndpoint) {
            RemoteMessageEndpoint remoteMEP = (RemoteMessageEndpoint) mep;
            remoteMEP.shutdown();
        }
    }
}

package org.xadisk.filesystem;

import java.lang.reflect.Method;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import org.xadisk.connector.EventProcessingXAResource;
import org.xadisk.connector.FileSystemEventListener;

class FileSystemEventProcessor implements Work {

    private final MessageEndpointFactory mef;
    private final FileStateChangeEvent event;
    private final XAFileSystem xaFileSystem;

    FileSystemEventProcessor(MessageEndpointFactory mef, FileStateChangeEvent event, XAFileSystem xaFileSystem) {
        this.mef = mef;
        this.event = event;
        this.xaFileSystem = xaFileSystem;
    }

    @Override
    public void release() {
    }

    @Override
    public void run() {
        EventProcessingXAResource epXAR = new EventProcessingXAResource(xaFileSystem, event);
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
        }
        mep.release();
    }
}

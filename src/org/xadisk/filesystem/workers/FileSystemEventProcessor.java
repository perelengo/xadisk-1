package org.xadisk.filesystem.workers;

import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;
import javax.transaction.Status;
import org.xadisk.bridge.proxies.impl.RemoteMessageEndpoint;
import org.xadisk.bridge.proxies.impl.RemoteMessageEndpointFactory;
import org.xadisk.bridge.server.conversation.HostedContext;
import org.xadisk.connector.inbound.LocalEventProcessingXAResource;
import org.xadisk.connector.inbound.FileSystemEventListener;
import org.xadisk.filesystem.FileStateChangeEvent;
import org.xadisk.filesystem.NativeXAFileSystem;

public class FileSystemEventProcessor implements Work {

    private final MessageEndpointFactory mef;
    private final FileStateChangeEvent event;
    private final NativeXAFileSystem xaFileSystem;
    private final LinkedBlockingQueue<FileStateChangeEvent> eventQueue;

    FileSystemEventProcessor(MessageEndpointFactory mef, FileStateChangeEvent event, NativeXAFileSystem xaFileSystem,
            LinkedBlockingQueue<FileStateChangeEvent> eventQueue) {
        this.mef = mef;
        this.event = event;
        this.xaFileSystem = xaFileSystem;
        this.eventQueue = eventQueue;
    }

    public void release() {
    }

    public void run() {
        LocalEventProcessingXAResource epXAR = new LocalEventProcessingXAResource(xaFileSystem, event);
        MessageEndpoint mep = null;
        try {
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
            Method methodToInvoke = FileSystemEventListener.class.getMethod("onFileSystemEvent",
                    FileStateChangeEvent.class);
            mep.beforeDelivery(methodToInvoke);
            methodToInvoke.invoke(mep, event);
            mep.afterDelivery();

            if(mef instanceof RemoteMessageEndpointFactory) {
                HostedContext globalCallbackContext = NativeXAFileSystem.getXAFileSystem().getGlobalCallbackContext();
                globalCallbackContext.deHostObject(epXAR);
            }

            mep.release();
            if (mep instanceof RemoteMessageEndpoint) {
                RemoteMessageEndpoint remoteMEP = (RemoteMessageEndpoint) mep;
                remoteMEP.shutdown();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (epXAR.getTransactionOutcome() != Status.STATUS_COMMITTED) {
                try {
                    xaFileSystem.getDeadLetter().dumpAndCommitMessage(event, epXAR);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }
}

package org.xadisk.tests.performance;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;

public class FileTruncater extends TimeMeasuredWork {

    private File filePath;

    public FileTruncater(File filePath, AtomicLong timeTaken, boolean useXADisk) {
        super(timeTaken, useXADisk);
        this.filePath = filePath;
    }

    @Override
    protected void doWorkDirectly() throws Exception {
        FileChannel fc = new FileOutputStream(filePath, true).getChannel();
        fc.truncate((long) (Appraiser.FILE_SIZE * 0.9));
        fc.force(false);
        fc.close();
    }

    @Override
    protected void doWorkViaXADisk() throws Exception {
        XAFileSystem xafs = XAFileSystemProxy.getNativeXAFileSystemReference("");
        Session session = xafs.createSessionForLocalTransaction();
        session.truncateFile(filePath, (long) (Appraiser.FILE_SIZE * 0.9));
        session.commit();
    }
}

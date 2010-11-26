package org.xadisk.tests;

import java.io.File;
import org.xadisk.bridge.proxies.impl.RemoteXAFileSystem;
import org.xadisk.bridge.proxies.interfaces.Session;

public class RemoteXADiskEnqueuer {
    public static void main(String args[]) {
        try {
            RemoteXAFileSystem xafs = new RemoteXAFileSystem("localhost", 9998);
            Session s = xafs.createSessionForLocalTransaction();
            String TEST_ROOT = "C:";
            File f = new File(TEST_ROOT + "\\a.txt");
            if(s.fileExists(f, true)) {
                s.deleteFile(f);
            }
            s.createFile(f, true);
            s.setPublishFileStateChangeEventsOnCommit(true);
            s.commit();

            xafs.shutdown();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}

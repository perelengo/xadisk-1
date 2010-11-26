package org.xadisk.tests;

import java.io.File;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

public class RemoteXADiskBootup {

    public static void main(String args[]) {
        try {
            int port;
            if (args.length > 0) {
                port = Integer.valueOf(args[0]);
            } else {
                port = Configuration.getRemoteServerPort();
            }
            String XADiskSystemDirectory = Configuration.getXADiskSystemDirectory() + "Remote#" + port;
            StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(XADiskSystemDirectory);
            configuration.setWorkManagerCorePoolSize(100);
            configuration.setWorkManagerMaxPoolSize(100);
            configuration.setTransactionTimeout(Integer.MAX_VALUE);
            configuration.setServerPort(port);
            TestUtility.cleanupDirectory(new File(XADiskSystemDirectory));
            NativeXAFileSystem xaFileSystem = NativeXAFileSystem.bootXAFileSystemStandAlone(configuration);
            xaFileSystem.waitForBootup(-1L);

            System.out.println("XADisk System is up for use...");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.tests.correctness;

import java.io.File;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

public class RemoteXADiskBootup {

    public static final int DEFAULT_PORT = 5151;
    public static boolean cleanSystemDir = false;
    
    public static void main(String args[]) {
        try {
            int port;
            if (args.length > 0) {
                port = Integer.valueOf(args[0]);
            } else {
                port = DEFAULT_PORT;
            }
            String XADiskSystemDirectory = Configuration.getXADiskSystemDirectory() + "Remote#" + port;
            if(cleanSystemDir) {
                TestUtility.cleanupDirectory(new File(XADiskSystemDirectory));
            }
            StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(XADiskSystemDirectory, "");
            configuration.setWorkManagerCorePoolSize(100);
            configuration.setWorkManagerMaxPoolSize(100);
            configuration.setTransactionTimeout(Integer.MAX_VALUE);
            configuration.setServerPort(port);
            configuration.setEnableRemoteInvocations(true);
            NativeXAFileSystem xaFileSystem = NativeXAFileSystem.bootXAFileSystemStandAlone(configuration);
            xaFileSystem.waitForBootup(-1L);

            System.out.println("XADisk System is up for use...");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

package org.xadisk.tests.correctness;

import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

public class TestMiscAPIs {

    public static void main(String args[]) {
        try {
            boolean testRemote = false;
            int remotePort = 4679;
            StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration("C:\\apis", "1");
            configuration.setEnableRemoteInvocations(Boolean.TRUE);
            configuration.setServerPort(remotePort);
            XAFileSystem xafs;
            xafs = XAFileSystemProxy.bootNativeXAFileSystem(configuration);
            xafs.waitForBootup(-1);
            if (testRemote) {
                xafs = XAFileSystemProxy.getRemoteXAFileSystemReference("localhost", remotePort);
            }
            xafs.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

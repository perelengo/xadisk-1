package org.xadisk.tests;

import java.io.File;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.impl.RemoteXAFileSystem;
import org.xadisk.bridge.proxies.impl.XADiskRemoteManagedConnectionFactory;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

public class TestRemoteXADiskRecover {

    private static final String SEPERATOR = File.separator;
    private static final String currentWorkingDirectory = System.getProperty("user.dir");
    private static final String XADiskSystemDirectory = currentWorkingDirectory + SEPERATOR + "XADiskSystem";

    public static void main(String args[]) {
        try {
            StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(XADiskSystemDirectory);
            configuration.setWorkManagerCorePoolSize(100);
            configuration.setWorkManagerMaxPoolSize(100);
            configuration.setServerPort(9998);

            NativeXAFileSystem xaFileSystem = NativeXAFileSystem.bootXAFileSystemStandAlone(configuration);
            xaFileSystem.waitForBootup(-1L);

            System.out.println("XADisk System is up for use...");

            Session session = xaFileSystem.createSessionForLocalTransaction();
            File c = new File("C:\\remotelyCreated.txt");

            if (session.fileExists(c, true)) {
                session.deleteFile(c);
            }
            session.createFile(c, false);

            session.setPublishFileStateChangeEventsOnCommit(true);
            session.prepare();
            xaFileSystem.shutdown();

            xaFileSystem = NativeXAFileSystem.bootXAFileSystemStandAlone(configuration);

            XAFileSystem remoteXAFileSystem = new RemoteXAFileSystem("localhost", 9998);

            try {
                remoteXAFileSystem.waitForBootup(5000);
            } catch (Exception e) {
                Xid xids[] = remoteXAFileSystem.recover(XAResource.TMSTARTRSCAN);
                XADiskRemoteManagedConnectionFactory mcf = new XADiskRemoteManagedConnectionFactory();
                mcf.setServerAddress("localhost");
                mcf.setServerPort(9998);
                mcf.createManagedConnection(null, null).getXAResource().commit(xids[0], true);
            }

            try {
                remoteXAFileSystem.waitForBootup(5000);
            } catch (Exception e) {
                System.err.println("UNEXPECTED.");
            }

            System.out.println("XADisk System is up for use...");
            xaFileSystem.shutdown();

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

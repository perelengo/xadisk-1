package org.xadisk.tests;

import java.io.File;
import org.xadisk.bridge.proxies.impl.RemoteXAFileSystem;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.connector.inbound.EndPointActivation;
import org.xadisk.connector.inbound.XADiskActivationSpecImpl;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

public class TestEventing {
    private static final String SEPERATOR = File.separator;
    private static final String currentWorkingDirectory = System.getProperty("user.dir");
    private static final String XADiskSystemDirectory = currentWorkingDirectory + SEPERATOR + "XADiskSystem2";

    public static void main(String args[]) {
        try {
            TestUtility.cleanupDirectory(new File(XADiskSystemDirectory));
            File f = new File("C:\\a.txt");
            XADiskActivationSpecImpl as = new XADiskActivationSpecImpl();
            as.setAreFilesRemote("true");
            as.setRemoteServerAddress("localhost");
            as.setRemoteServerPort("9999");
            as.setFileNamesAndEventInterests(f.getAbsolutePath() + "::111");
            
            StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(XADiskSystemDirectory);
            configuration.setWorkManagerCorePoolSize(100);
            configuration.setWorkManagerMaxPoolSize(100);
            configuration.setServerPort(1111);
            NativeXAFileSystem xaFileSystem = NativeXAFileSystem.bootXAFileSystemStandAlone(configuration);
            xaFileSystem.waitForBootup(-1L);
            System.out.println("XADisk System is up...");

            RemoteXAFileSystem rXAFS = new RemoteXAFileSystem("localhost", 9999);
            SimulatedMessageEndpointFactory smef = new SimulatedMessageEndpointFactory();
            smef.goTill = SimulatedMessageEndpointFactory.GoTill.consume;
            EndPointActivation epa = new EndPointActivation(smef, as);
            rXAFS.registerEndPointActivation(epa);

            Thread.sleep(10000);//reboot the remote xdisk here.
            
            Session session = xaFileSystem.createSessionForLocalTransaction();
            session.setPublishFileStateChangeEventsOnCommit(true);
            if(session.fileExists(f, true)) {
                session.deleteFile(f);
            }
            session.createFile(f, false);
            session.commit();
            
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

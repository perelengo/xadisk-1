/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.tests.correctness;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.bridge.proxies.impl.RemoteXAFileSystem;
import org.xadisk.bridge.proxies.impl.XADiskRemoteManagedConnectionFactory;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.connector.inbound.EndPointActivation;
import org.xadisk.connector.inbound.XADiskActivationSpecImpl;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.SessionCommonness;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

public class TestRemoteXADiskRecover {

    private static final String SEPERATOR = File.separator;
    private static final String currentWorkingDirectory = "C:\\";
    private static final String XADiskSystemDirectory = currentWorkingDirectory + SEPERATOR + "XADiskSystem1";
    private static final String RemoteXADiskSystemDirectory = currentWorkingDirectory + SEPERATOR + "XADiskSystem";

    public static void main(String args[]) {
        try {
            TestUtility.cleanupDirectory(new File(XADiskSystemDirectory));
            TestUtility.cleanupDirectory(new File(RemoteXADiskSystemDirectory));

            System.out.println("Remember to set the cleanupSystemDir flag in RemoteXADiskBootup class to false");
            NativeXAFileSystem nativeXAFS = bootLocalXADisk();

            Process remoteXADisk = bootRemoteXADisk();
            Thread.sleep(4000);

            SimulatedMessageEndpointFactory mef = new SimulatedMessageEndpointFactory();
            mef.goTill = SimulatedMessageEndpointFactory.GoTill.commit;
            XADiskActivationSpecImpl as = new XADiskActivationSpecImpl();
            as.setAreFilesRemote("true");
            as.setRemoteServerAddress("localhost");
            as.setRemoteServerPort(RemoteXADiskBootup.DEFAULT_PORT + "");
            as.setFileNamesAndEventInterests(currentWorkingDirectory + "\\::111");
            EndPointActivation activation = new EndPointActivation(mef, as);

            RemoteXAFileSystem xafs = new RemoteXAFileSystem("localhost", RemoteXADiskBootup.DEFAULT_PORT, NativeXAFileSystem.getXAFileSystem(""));
            xafs.registerEndPointActivation(activation);

            Session session = xafs.createSessionForLocalTransaction();
            File c = new File(currentWorkingDirectory + "\\remotelyCreated.txt");
            if (session.fileExists(c, true)) {
                session.deleteFile(c);
            }
            session.createFile(c, false);
            session.setPublishFileStateChangeEventsOnCommit(true);
            session.commit();

            session = xafs.createSessionForLocalTransaction();
            c = new File(currentWorkingDirectory + "\\remotelyCreated.txt");
            if (session.fileExists(c, true)) {
                session.deleteFile(c);
            }
            session.createFile(c, false);
            session.setPublishFileStateChangeEventsOnCommit(true);
            ((SessionCommonness) session).prepare();

            createSpaceForMessageDelivery();

            System.out.println("Crash...");
            crashRemoteXADisk(remoteXADisk);

            remoteXADisk = bootRemoteXADisk();

            Thread.sleep(2000);

            xafs = new RemoteXAFileSystem("localhost", RemoteXADiskBootup.DEFAULT_PORT, NativeXAFileSystem.getXAFileSystem(""));
            Xid xids[] = xafs.recover(XAResource.TMSTARTRSCAN);
            XADiskRemoteManagedConnectionFactory mcf = new XADiskRemoteManagedConnectionFactory();
            ((XADiskRemoteManagedConnectionFactory) mcf).setInstanceId("");
            mcf.setServerAddress("localhost");
            mcf.setServerPort(RemoteXADiskBootup.DEFAULT_PORT);
            XAResource xar = mcf.createManagedConnection(null, null).getXAResource();
            for (Xid xid : xids) {
                System.out.println("Committing after crash...");
                xar.commit(xid, true);
            }
            createSpaceForMessageDelivery();

            xafs.deRegisterEndPointActivation(activation);
            xafs.registerEndPointActivation(activation);

            session = xafs.createSessionForLocalTransaction();
            c = new File(currentWorkingDirectory + "\\remotelyCreated.txt");
            if (session.fileExists(c, true)) {
                session.deleteFile(c);
            }
            session.createFile(c, false);
            session.setPublishFileStateChangeEventsOnCommit(true);
            session.commit();

            createSpaceForMessageDelivery();

            crashRemoteXADisk(remoteXADisk);
            remoteXADisk = bootRemoteXADisk();
            Thread.sleep(2000);
            xafs = new RemoteXAFileSystem("localhost", RemoteXADiskBootup.DEFAULT_PORT, NativeXAFileSystem.getXAFileSystem(""));

            session = xafs.createSessionForLocalTransaction();
            c = new File(currentWorkingDirectory + "\\remotelyCreated.txt");
            if (session.fileExists(c, true)) {
                session.deleteFile(c);
            }
            session.createFile(c, false);
            session.setPublishFileStateChangeEventsOnCommit(true);
            session.commit();

            createSpaceForMessageDelivery();

            crashRemoteXADisk(remoteXADisk);
            nativeXAFS.shutdown();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static Process bootRemoteXADisk() throws IOException {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("java");
        commands.add("-classpath");
        commands.add("build\\classes;connector-api-1.5.jar");
        commands.add(org.xadisk.tests.correctness.RemoteXADiskBootup.class.getName());
        ProcessBuilder pb = new ProcessBuilder(commands);
        Process p = pb.start();
        new Thread(new ChildProcessOutputStreamReader(p.getInputStream(), System.out)).start();
        new Thread(new ChildProcessOutputStreamReader(p.getErrorStream(), System.err)).start();
        return p;
    }

    private static void crashRemoteXADisk(Process p) throws IOException {
        p.destroy();
        p.getErrorStream().close();
        p.getInputStream().close();
        p.getOutputStream().close();
    }

    private static NativeXAFileSystem bootLocalXADisk() throws InterruptedException {
        StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(XADiskSystemDirectory, "");
        configuration.setWorkManagerCorePoolSize(100);
        configuration.setWorkManagerMaxPoolSize(100);
        configuration.setServerPort(2345);
        configuration.setEnableRemoteInvocations(true);
        NativeXAFileSystem nativeXAFS = NativeXAFileSystem.bootXAFileSystemStandAlone(configuration);
        nativeXAFS.waitForBootup(-1);
        System.out.println("Booted local...");
        return nativeXAFS;
    }

    private static void createSpaceForMessageDelivery() throws InterruptedException {
        System.out.println("Expecting message delivery here...");
        Thread.sleep(3000);
    }
}

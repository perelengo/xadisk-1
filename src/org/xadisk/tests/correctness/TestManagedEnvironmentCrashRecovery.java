/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.tests.correctness;

import java.io.File;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.connector.inbound.EndPointActivation;
import org.xadisk.connector.inbound.XADiskActivationSpecImpl;
import org.xadisk.connector.outbound.XADiskConnection;
import org.xadisk.connector.outbound.XADiskManagedConnection;
import org.xadisk.connector.outbound.XADiskManagedConnectionFactory;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.XAFileSystemCommonness;
import org.xadisk.filesystem.XidImpl;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;
import org.xadisk.filesystem.utilities.FileIOUtility;

public class TestManagedEnvironmentCrashRecovery {

    private static final String SEPERATOR = File.separator;
    private static final String currentWorkingDirectory = "C:\\";
    private static final String XADiskSystemDirectory = currentWorkingDirectory + SEPERATOR + "XADiskSystem";
    private static long txnId = System.currentTimeMillis();

    public static void main(String args[]) {
        XAFileSystemCommonness xaFileSystem = null;
        try {
            boolean commitDuringRecovery = false;
            File f[] = new File[3];
            for (int i = 0; i < 3; i++) {
                File dir = new File(currentWorkingDirectory + SEPERATOR + i);
                FileIOUtility.deleteDirectoryRecursively(dir);
                FileIOUtility.createDirectory(dir);
                f[i] = new File(dir, "a.txt");
                if(f[i].exists()) {
                    FileIOUtility.deleteFile(f[i]);
                }
            }

            xaFileSystem = bootXAFileSystemCompletely();

            XADiskManagedConnection mc = (XADiskManagedConnection) getXADiskManagedConnectionFactory().createManagedConnection(null, null);
            XAResource xar = mc.getXAResource();
            Xid xid = getNewXid();
            xar.start(xid, XAResource.TMNOFLAGS);
            XADiskConnection connection = (XADiskConnection) mc.getConnection(null, null);
            connection.createFile(f[0], false);
            xar.end(xid, XAResource.TMSUCCESS);
            assertNoCommit(f[0]);

            mc = (XADiskManagedConnection) getXADiskManagedConnectionFactory().createManagedConnection(null, null);
            xar = mc.getXAResource();
            xid = getNewXid();
            xar.start(xid, XAResource.TMNOFLAGS);
            connection = (XADiskConnection) mc.getConnection(null, null);
            connection.createFile(f[1], false);
            xar.end(xid, XAResource.TMSUCCESS);
            xar.prepare(xid);
            assertNoCommit(f[1]);

            mc = (XADiskManagedConnection) getXADiskManagedConnectionFactory().createManagedConnection(null, null);
            xar = mc.getXAResource();
            xid = getNewXid();
            xar.start(xid, XAResource.TMNOFLAGS);
            connection = (XADiskConnection) mc.getConnection(null, null);
            connection.createFile(f[2], false);
            xar.end(xid, XAResource.TMSUCCESS);
            xar.prepare(xid);
            xar.commit(xid, false);
            assertCommit(f[2]);

            xaFileSystem.shutdown();
            xaFileSystem = bootXAFileSystem();
            Thread.sleep(1000);

            mc = (XADiskManagedConnection) getXADiskManagedConnectionFactory().createManagedConnection(null, null);
            xar = mc.getXAResource();
            Xid[] preparedXids = xaFileSystem.recover(XAResource.TMSTARTRSCAN);
            for (Xid prepared : preparedXids) {
                if (commitDuringRecovery) {
                    xar.commit(prepared, true);
                } else {
                    xar.rollback(prepared);
                }
            }

            assertNoCommit(f[0]);
            if (commitDuringRecovery) {
                assertCommit(f[1]);
                assertCommit(f[2]);
            }

            xaFileSystem.shutdown();
            //a "Complete" starting xadisk indicates no in-doubt txns.
            xaFileSystem = bootXAFileSystemCompletely();

            for(int i = 0; i < 3; i++) {
                if(f[i].exists()) {
                    FileIOUtility.deleteFile(f[i]);
                }
            }

            SimulatedMessageEndpointFactory smef = new SimulatedMessageEndpointFactory();
            smef.goTill = SimulatedMessageEndpointFactory.GoTill.consume;
            XADiskActivationSpecImpl as = new XADiskActivationSpecImpl();
            as.setAreFilesRemote("false");
            as.setFileNamesAndEventInterests(new File(currentWorkingDirectory + SEPERATOR + 0).getAbsolutePath()
                    + "::111");
            xaFileSystem.registerEndPointActivation(new EndPointActivation(smef, as));

            smef = new SimulatedMessageEndpointFactory();
            smef.goTill = SimulatedMessageEndpointFactory.GoTill.prepare;
            as = new XADiskActivationSpecImpl();
            as.setAreFilesRemote("false");
            as.setFileNamesAndEventInterests(new File(currentWorkingDirectory + SEPERATOR + 1).getAbsolutePath()
                    + "::111");
            xaFileSystem.registerEndPointActivation(new EndPointActivation(smef, as));

            smef = new SimulatedMessageEndpointFactory();
            smef.goTill = SimulatedMessageEndpointFactory.GoTill.commit;
            as = new XADiskActivationSpecImpl();
            as.setAreFilesRemote("false");
            as.setFileNamesAndEventInterests(new File(currentWorkingDirectory + SEPERATOR + 2).getAbsolutePath()
                    + "::111");
            xaFileSystem.registerEndPointActivation(new EndPointActivation(smef, as));

            mc = (XADiskManagedConnection) getXADiskManagedConnectionFactory().createManagedConnection(null, null);
            xar = mc.getXAResource();
            xid = getNewXid();
            xar.start(xid, XAResource.TMNOFLAGS);
            connection = (XADiskConnection) mc.getConnection(null, null);
            connection.createFile(f[0], false);
            connection.createFile(f[1], false);
            connection.createFile(f[2], false);
            connection.setPublishFileStateChangeEventsOnCommit(true);
            xar.end(xid, XAResource.TMSUCCESS);
            xar.prepare(xid);
            xar.commit(xid, false);

            Thread.sleep(1000);//to let the events get raised to MEPs.

            xaFileSystem.shutdown();
            xaFileSystem = bootXAFileSystem();
            Thread.sleep(1000);

            xar = xaFileSystem.getEventProcessingXAResourceForRecovery();
            preparedXids = xar.recover(XAResource.TMSTARTRSCAN);
            if (preparedXids.length != 1) {
                throw new AssertionError();
            }
            System.out.println("Committing the only one expected prepared txn for inbound case.");
            if (commitDuringRecovery) {
                xar.commit(preparedXids[0], true);
            } else {
                xar.rollback(preparedXids[0]);
            }
            
            xaFileSystem.shutdown();
            xaFileSystem = bootXAFileSystemCompletely();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                xaFileSystem.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static XADiskManagedConnectionFactory getXADiskManagedConnectionFactory() {
        XADiskManagedConnectionFactory mcf = new XADiskManagedConnectionFactory();
        ((XADiskManagedConnectionFactory) mcf).setInstanceId("");
        return mcf;
    }

    private static Xid getNewXid() {
        return XidImpl.getXidInstanceForLocalTransaction(txnId++);
    }

    private static void assertCommit(File f) {
        if (!f.exists()) {
            throw new AssertionError();
        }
    }

    private static void assertNoCommit(File f) {
        if (f.exists()) {
            throw new AssertionError();
        }
    }

    private static XAFileSystemCommonness bootXAFileSystem() throws Exception {
        XAFileSystemCommonness xaFS;
        StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(XADiskSystemDirectory, "");
        configuration.setServerPort(9998);
        xaFS = NativeXAFileSystem.bootXAFileSystemStandAlone(configuration);
        return xaFS;
    }

    private static XAFileSystemCommonness bootXAFileSystemCompletely() throws Exception {
        XAFileSystemCommonness xaFS = bootXAFileSystem();
        xaFS.waitForBootup(-1L);
        System.out.println("XADisk System is up...");
        return xaFS;
    }
}

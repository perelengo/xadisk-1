package org.xadisk.tests;

import java.io.File;
import java.io.IOException;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.connector.ConnectionHandle;
import org.xadisk.connector.ManagedConnectionFactoryImpl;
import org.xadisk.filesystem.FileIOUtility;
import org.xadisk.filesystem.StandaloneFileSystemConfiguration;
import org.xadisk.filesystem.XAFileSystem;
import org.xadisk.filesystem.XidImpl;

public class ManagedEnvironmentSpecificTests {

    public static void main(String args[]) {
        XAFileSystem xaFileSystem = null;
        try {
            final String currentWorkingDirectory = System.getProperty("user.dir");
            final String SEPERATOR = File.separator;
            long plainTransactionId = System.currentTimeMillis();
            File testRootDirectory = new File(currentWorkingDirectory, "testXADiskManaged");
            File testDirectory1 = new File(currentWorkingDirectory, "testXADiskManaged" + SEPERATOR + "dir1");
            File testDirectory2 = new File(currentWorkingDirectory + SEPERATOR + "testXADiskManaged" + SEPERATOR + "dir2");
            File systemDirectory = new File(currentWorkingDirectory, "xaDiskHome");
            FileIOUtility.deleteDirectoryRecursively(testRootDirectory);
            FileIOUtility.createDirectory(testRootDirectory);
            FileIOUtility.createDirectory(testDirectory1);
            FileIOUtility.createDirectory(testDirectory2);
            FileIOUtility.deleteDirectoryRecursively(systemDirectory);
            StandaloneFileSystemConfiguration saConfig =
                    new StandaloneFileSystemConfiguration(systemDirectory.getAbsolutePath());

            xaFileSystem = XAFileSystem.bootXAFileSystemStandAlone(saConfig);
            waitForXAFileSystemToBeUp(xaFileSystem);

            ManagedConnectionFactory mcf = new ManagedConnectionFactoryImpl();
            ManagedConnection mc = mcf.createManagedConnection(null, null);
            XAResource xar = mc.getXAResource();
            XAResource xarSameRM = mcf.createManagedConnection(null, null).getXAResource();
            Xid xid = getNewXid(plainTransactionId++);
            ConnectionHandle connection = (ConnectionHandle) mc.getConnection(null, null);

            xar.start(xid, XAResource.TMNOFLAGS);
            doTransactionalWork(connection, testDirectory1, "a.txt");
            xar.end(xid, XAResource.TMSUCCESS);
            xar.prepare(xid);
            xar.commit(xid, false);

            xid = getNewXid(plainTransactionId++);
            xar.start(xid, XAResource.TMNOFLAGS);
            doTransactionalWork(connection, testDirectory1, "b.txt");
            xar.end(xid, XAResource.TMSUSPEND);
            Xid temp = getNewXid(plainTransactionId++);
            xar.start(temp, XAResource.TMNOFLAGS);
            doTransactionalWork(connection, testDirectory2, "c.txt");
            xar.end(temp, XAResource.TMSUSPEND);
            xar.start(xid, XAResource.TMRESUME);
            doTransactionalWork(connection, testDirectory1, "d.txt");
            xar.end(xid, XAResource.TMSUCCESS);
            xar.start(temp, XAResource.TMRESUME);
            doTransactionalWork(connection, testDirectory2, "e.txt");
            xar.end(temp, XAResource.TMSUCCESS);
            xarSameRM.commit(temp, true);
            xarSameRM.prepare(xid);
            xarSameRM.rollback(xid);

            xid = getNewXid(plainTransactionId++);
            xar.start(xid, XAResource.TMNOFLAGS);
            doTransactionalWork(connection, testDirectory1, "f1.txt");
            xar.end(xid, XAResource.TMSUCCESS);
            xar.prepare(xid);
            xid = getNewXid(plainTransactionId++);
            xar.start(xid, XAResource.TMNOFLAGS);
            doTransactionalWork(connection, testDirectory2, "f2.txt");
            xar.end(xid, XAResource.TMSUCCESS);
            xaFileSystem.shutdown();
            xaFileSystem = XAFileSystem.bootXAFileSystemStandAlone(saConfig);
            Thread.sleep(5000);
            mcf = new ManagedConnectionFactoryImpl();
            mc = mcf.createManagedConnection(null, null);
            xar = mc.getXAResource();

            Xid preparedBeforeCrash[] = xar.recover(XAResource.TMNOFLAGS);
            for (int i = 0; i < preparedBeforeCrash.length; i++) {
                System.out.println("Committing txn prepared before crash...");
                xar.commit(preparedBeforeCrash[i], false);
            }

            waitForXAFileSystemToBeUp(xaFileSystem);
            System.out.println("The XAFileSystem is up-And-running again after crash.");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (xaFileSystem != null) {
                try {
                    xaFileSystem.shutdown();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    private static void doTransactionalWork(ConnectionHandle connection, File testDirectory,
            String fileName)
            throws Exception {
        connection.createFile(new File(testDirectory, fileName), false);
    }

    private static Xid getNewXid(long plainTransactionId) {
        return XidImpl.getXidInstanceForLocalTransaction(plainTransactionId);
    }

    private static void waitForXAFileSystemToBeUp(XAFileSystem xaFileSystem) {
        while (true) {
            try {
                Thread.sleep(1000);
                xaFileSystem.checkIfCanContinue();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

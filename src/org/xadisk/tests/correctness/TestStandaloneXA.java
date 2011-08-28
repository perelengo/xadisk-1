package org.xadisk.tests.correctness;

import java.io.File;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XAFileSystemProxy;
import org.xadisk.bridge.proxies.interfaces.XASession;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

/*For testing, we used Atomikos (open source version) as a JTA implementation. One can get it from
http://www.atomikos.com/Main/TransactionsEssentials .
 */
public class TestStandaloneXA {

    public static void main(String args[]) {
        try {
            boolean testRemote = false;
            int remotePort = 4678;
            StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration("C:\\xa", "1");
            configuration.setEnableRemoteInvocations(Boolean.TRUE);
            configuration.setServerPort(remotePort);
            XAFileSystem xafs;
            XAFileSystem nativeXAFS = XAFileSystemProxy.bootNativeXAFileSystem(configuration);
            nativeXAFS.waitForBootup(-1);
            if (testRemote) {
                xafs = XAFileSystemProxy.getRemoteXAFileSystemReference("localhost", remotePort);
            } else {
                xafs = nativeXAFS;
            }
            XASession xaSession = xafs.createSessionForXATransaction();
            XAResource xar = xaSession.getXAResource();
            //TransactionManager tm = new com.atomikos.icatch.jta.UserTransactionManager();
            //UNCOMMENT ABOVE ONCE YOU BRING ATOMIKOS INTO THE CLASSPATH.
            TransactionManager tm = null;
            tm.setTransactionTimeout(60);

            tm.begin();
            Transaction tx1 = tm.getTransaction();
            tx1.enlistResource(xar);//==> xar.start
            xaSession.createFile(new File("C:\\a.txt"), false);
            tm.suspend();//==> xar.end

            tm.begin();
            Transaction tx2 = tm.getTransaction();
            tx2.enlistResource(xar);//==> xar.start
            xaSession.createFile(new File("C:\\b.txt"), false);
            tm.commit();//test rollback also.//==> xar.end

            tm.resume(tx1);
            tx1.enlistResource(xar);//==> xar.start
            xaSession.createFile(new File("C:\\c.txt"), false);
            tm.commit();//test rollback also.//==> xar.end

            nativeXAFS.shutdown();
            nativeXAFS = XAFileSystemProxy.bootNativeXAFileSystem(configuration);

            if (testRemote) {
                xafs = XAFileSystemProxy.getRemoteXAFileSystemReference("localhost", remotePort);
            } else {
                xafs = nativeXAFS;
            }
            Xid xids[] = xafs.getXAResourceForRecovery().recover(XAResource.TMSTARTRSCAN);
            System.out.println(xids.length);
            nativeXAFS.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

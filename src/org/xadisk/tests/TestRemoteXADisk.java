package org.xadisk.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;
import org.xadisk.bridge.proxies.impl.RemoteXAFileSystem;

public class TestRemoteXADisk {

    private static final String SEPERATOR = File.separator;
    private static final String currentWorkingDirectory = System.getProperty("user.dir");
    private static final String XADiskSystemDirectory = currentWorkingDirectory + SEPERATOR + "XADiskSystem";
    private static final String topLevelTestDirectory = currentWorkingDirectory + SEPERATOR + "testXADiskSystem";

    public static void main(String args[]) {
        try {
            TestUtility.remoteXAFileSystem = true;
            
            StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(XADiskSystemDirectory);
            configuration.setWorkManagerCorePoolSize(100);
            configuration.setWorkManagerMaxPoolSize(100);
            configuration.setTransactionTimeout(Integer.MAX_VALUE);
            configuration.setServerPort(9999);
            NativeXAFileSystem localXAFileSystem = NativeXAFileSystem.bootXAFileSystemStandAlone(configuration);
            localXAFileSystem.waitForBootup(-1L);

            new File(topLevelTestDirectory).mkdirs();

            XAFileSystem remoteXAFileSystem = new RemoteXAFileSystem("localhost", 9999);

            remoteXAFileSystem.waitForBootup(-1L);
            
            Thread tests[] = new Thread[4];
            ArrayList<Thread> allThreads = new ArrayList<Thread>();

            tests[0] = new Thread(new RunnableTest(CoreXAFileSystemTests.testConcurrentMoneyTransfer,
                    topLevelTestDirectory + SEPERATOR + "moneyTransfer"));
            tests[1] = new Thread(new RunnableTest(CoreXAFileSystemTests.testDynamicReadWrite,
                    topLevelTestDirectory + SEPERATOR + "dynamicReadWrite"));
            tests[2] = new Thread(new RunnableTest(CoreXAFileSystemTests.testIncrementalIOOperations,
                    topLevelTestDirectory + SEPERATOR + "ioOperations"));
            tests[3] = new Thread(new RunnableTest(CoreXAFileSystemTests.testFileSystemEventing,
                    topLevelTestDirectory + SEPERATOR + "eventingSystems"));

            for (int i = 0; i < 4; i++) {
                if (i == 2) {
                    tests[i].start();
                    allThreads.add(tests[i]);
                }
            }

            TestUtility.waitForAllAtHeaven(allThreads);

            remoteXAFileSystem.shutdown();

            localXAFileSystem.shutdown();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

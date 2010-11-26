package org.xadisk.tests;

public class BeforeYouCommitYourEfforts {
    public static void main(String args[]) {
        try {
            CoreXAFileSystemTests.testHighNumber = false;
            CoreXAFileSystemTests.testProgressive = false;
            CoreXAFileSystemTests.usePessimisticLock = true;
            TestUtility.remoteXAFileSystem = true;

            TestCoreXAFileSystem.testCrashRecovery = false;
            TestCoreXAFileSystem.concurrencyLevel = 1;
            TestCoreXAFileSystem.numberOfCrashes = 100;
            TestCoreXAFileSystem.maxConcurrentDeliveries = 2;

            TestCoreXAFileSystem.main(new String[0]);

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}

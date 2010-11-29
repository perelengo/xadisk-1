package org.xadisk.tests;

import java.util.ArrayList;
import java.util.List;

public class BeforeYouCommitYourEfforts {

    public static void main(String args[]) {
        try {
            CoreXAFileSystemTests.testHighNumber = false;
            CoreXAFileSystemTests.testProgressive = false;
            CoreXAFileSystemTests.usePessimisticLock = true;
            TestUtility.remoteXAFileSystem = false;

            TestCoreXAFileSystem.testCrashRecovery = true;
            TestCoreXAFileSystem.concurrencyLevel = 1;
            TestCoreXAFileSystem.numberOfCrashes = 100;
            TestCoreXAFileSystem.maxConcurrentDeliveries = 2;

            if (TestUtility.remoteXAFileSystem) {
                startRemoteXADisk();
            }

            TestCoreXAFileSystem.main(new String[0]);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startRemoteXADisk() throws Exception {
        List<String> argsList = new ArrayList<String>();
        argsList.add("java");
        argsList.add("-classpath");
        argsList.add("build/classes;connector-api-1.5.jar;javaee-api-5.jar");
        argsList.add(RemoteXADiskBootup.class.getName());
        TestUtility.remoteXADiskPort = Configuration.getNextServerPort();
        argsList.add(TestUtility.remoteXADiskPort + "");
        ProcessBuilder processBuilder = new ProcessBuilder(argsList);
        Process remoteXADiskProcess = processBuilder.start();
        new Thread(new ChildProcessOutputStreamReader(remoteXADiskProcess.getErrorStream(),
                System.err)).start();
        new Thread(new ChildProcessOutputStreamReader(remoteXADiskProcess.getInputStream(),
                System.out)).start();
        Thread.sleep(2000);
    }
}

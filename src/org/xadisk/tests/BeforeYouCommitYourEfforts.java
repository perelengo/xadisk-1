/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.tests;

import java.util.ArrayList;
import java.util.List;

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

            Process remoteXADisk = null;
            if (TestUtility.remoteXAFileSystem) {
                remoteXADisk = startRemoteXADisk();
                //for remote case, remember to delete the system dir so that the earlier activations, which didn't get de-activated
                //are not tried for raise messages and hence don't fail with exception like "No object with id".
            }

            TestCoreXAFileSystem.main(new String[0]);

            if(remoteXADisk != null) {
                remoteXADisk.destroy();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Process startRemoteXADisk() throws Exception {
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
        return remoteXADiskProcess;
    }
}

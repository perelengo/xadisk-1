/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.tests.correctness;

class RunnableTest implements Runnable {

    private CoreXAFileSystemTests.testNames testName;
    private String testDirectory;

    public RunnableTest(CoreXAFileSystemTests.testNames testName, String testDirectory) {
        this.testName = testName;
        this.testDirectory = testDirectory;
    }

    public void run() {
        try {
            if (testName.equals(CoreXAFileSystemTests.testNames.testConcurrentMoneyTransfer)) {
                CoreXAFileSystemTests.testConcurrentMoneyTransfer(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testNames.testIOOperations)) {
                CoreXAFileSystemTests.testIOOperations(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testNames.testDynamicReadWrite)) {
                CoreXAFileSystemTests.testDynamicReadWrite(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testNames.testFileSystemEventing)) {
                CoreXAFileSystemTests.testFileSystemEventing(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testNames.testConcurrentMoneyTransferPostCrash)) {
                CoreXAFileSystemTests.testConcurrentMoneyTransferPostCrash(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testNames.testDynamicReadWritePostCrash)) {
                CoreXAFileSystemTests.testDynamicReadWritePostCrash(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testNames.testIOOperationsPostCrash)) {
                CoreXAFileSystemTests.testIOOperationsPostCrash(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testNames.testFileSystemEventingPostCrash)) {
                CoreXAFileSystemTests.testFileSystemEventingPostCrash(testDirectory);
            }
        } catch (Throwable t) {
            System.out.println("Test failed " + testName + " in " + testDirectory + " due to " + t);
            t.printStackTrace();
        }
    }
}

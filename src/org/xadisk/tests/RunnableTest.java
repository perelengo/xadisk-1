package org.xadisk.tests;

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

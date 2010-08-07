package org.xadisk.tests;

class RunnableTest implements Runnable {

    private String testName;
    private String testDirectory;

    public RunnableTest(String testName, String testDirectory) {
        this.testName = testName;
        this.testDirectory = testDirectory;
    }

    public void run() {
        try {
            if (testName.equals(CoreXAFileSystemTests.testConcurrentMoneyTransfer)) {
                CoreXAFileSystemTests.testConcurrentMoneyTransfer(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testIOOperations)) {
                CoreXAFileSystemTests.testIOOperations(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testDynamicReadWrite)) {
                CoreXAFileSystemTests.testDynamicReadWrite(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testFileSystemEventing)) {
                CoreXAFileSystemTests.testFileSystemEventing(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testConcurrentMoneyTransferPostCrash)) {
                CoreXAFileSystemTests.testConcurrentMoneyTransferPostCrash(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testDynamicReadWritePostCrash)) {
                CoreXAFileSystemTests.testDynamicReadWritePostCrash(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testIOOperationsPostCrash)) {
                CoreXAFileSystemTests.testIOOperationsPostCrash(testDirectory);
            } else if (testName.equals(CoreXAFileSystemTests.testFileSystemEventingPostCrash)) {
                CoreXAFileSystemTests.testFileSystemEventingPostCrash(testDirectory);
            }
        } catch (Throwable t) {
            System.out.println("Test failed " + testName + " in " + testDirectory + " due to " + t);
            t.printStackTrace();
        }
    }
}

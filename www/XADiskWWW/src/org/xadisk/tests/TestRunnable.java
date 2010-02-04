package org.xadisk.tests;

class TestRunnable implements Runnable {

    private String testName;
    private String testDirectory;

    public TestRunnable(String testName, String testDirectory) {
        this.testName = testName;
        this.testDirectory = testDirectory;
    }

    @Override
    public void run() {
        try {
            int initialFileSize=100000;
            if (testName.equals(InspiredDiskIOs.testConcurrentMoneyTransfer)) {
                InspiredDiskIOs.testConcurrentMoneyTransfer(testDirectory, false);
            } else if (testName.equals(InspiredDiskIOs.testDynamicReadWrite)) {
                InspiredDiskIOs.testDynamicReadWrite(testDirectory, initialFileSize);
            } else if (testName.equals(InspiredDiskIOs.testIOOperations)) {
                InspiredDiskIOs.testIOOperations(testDirectory);
            } else if (testName.equals(InspiredDiskIOs.testFileSystemEventing)) {
                InspiredDiskIOs.testFileSystemEventing(testDirectory);
            } else if (testName.equals(InspiredDiskIOs.testConcurrentMoneyTransferPostCrash)) {
                InspiredDiskIOs.testConcurrentMoneyTransferPostCrash(testDirectory);
            } else if (testName.equals(InspiredDiskIOs.testDynamicReadWritePostCrash)) {
                InspiredDiskIOs.testDynamicReadWritePostCrash(testDirectory, initialFileSize);
            } else if (testName.equals(InspiredDiskIOs.testIOOperationsPostCrash)) {
                InspiredDiskIOs.testIOOperationsPostCrash(testDirectory);
            } else if (testName.equals(InspiredDiskIOs.testFileSystemEventingPostCrash)) {
                InspiredDiskIOs.testFileSystemEventingPostCrash(testDirectory);
            }
        } catch (Exception e) {
            System.out.println("Test failed " + testName + " in " + testDirectory + " due to " + e);
            e.printStackTrace();
        }
    }
}

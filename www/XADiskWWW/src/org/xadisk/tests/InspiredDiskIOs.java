package org.xadisk.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Random;
import org.xadisk.connector.ActivationSpecImpl;
import org.xadisk.connector.EndPointActivation;
import org.xadisk.filesystem.FileIOUtility;
import org.xadisk.filesystem.Session;
import org.xadisk.filesystem.XAFileInputStream;
import org.xadisk.filesystem.XAFileOutputStream;
import org.xadisk.filesystem.XAFileSystem;

public class InspiredDiskIOs {
    
    public static final String testIOOperations = "testIOOperations";
    public static final String testIOOperationsPostCrash = "testIOOperationsPostCrash";
    public static final String testDynamicReadWrite = "testDynamicReadWrite";
    public static final String testDynamicReadWritePostCrash = "testDynamicReadWritePostCrash";
    public static final String testConcurrentMoneyTransfer = "testConcurrentMoneyTransfer";
    public static final String testConcurrentMoneyTransferPostCrash = "testConcurrentMoneyTransferPostCrash";
    public static final String testFileSystemEventing = "testFileSystemEventing";
    public static final String testFileSystemEventingPostCrash = "testFileSystemEventingPostCrash";
    private static final String SEPERATOR = File.separator;

    public static void testIOOperations(String testDirectory) throws Exception {
        String roots[] = new String[2];
        roots[0] = testDirectory + SEPERATOR + "dir1";
        roots[1] = testDirectory + SEPERATOR + "dir2";
        File fileRoot1 = new File(roots[0]);
        File fileRoot2 = new File(roots[1]);
        TestUtility.cleanupDirectory(fileRoot1);
        TestUtility.cleanupDirectory(fileRoot2);
        fileRoot1.mkdirs();
        fileRoot2.mkdirs();

        Session session = XAFileSystem.getXAFileSystem().createSessionForLocalTransaction();

        TestUtility.createFile("childDir1", true, session, roots);
        TestUtility.createFile("childDir2", true, session, roots);

        TestUtility.createFile("childDir1" + SEPERATOR + "small.txt", false, session, roots);
        TestUtility.createFile("childDir1" + SEPERATOR + "medium.txt", false, session, roots);
        TestUtility.createFile("childDir1" + SEPERATOR + "large.txt", false, session, roots);

        byte chant[] = "HareRamaHareKrishna...".getBytes();
        int smallFileSize = 100;
        int mediumFileSize = 1000;
        int largeFileSize = 200000;
        TestUtility.writeDataToOutputStreams(chant, smallFileSize, "childDir1" + SEPERATOR + "small.txt", session, roots);
        TestUtility.writeDataToOutputStreams(chant, mediumFileSize, "childDir1" + SEPERATOR + "medium.txt", session, roots);
        TestUtility.writeDataToOutputStreams(chant, largeFileSize, "childDir1" + SEPERATOR + "large.txt", session, roots);

        TestUtility.moveFile("childDir1" + SEPERATOR + "small.txt", "childDir2" + SEPERATOR + "small.txt", session, roots);
        TestUtility.moveFile("childDir1" + SEPERATOR + "medium.txt", "childDir2" + SEPERATOR + "medium.txt", session, roots);
        TestUtility.moveFile("childDir1" + SEPERATOR + "large.txt", "childDir2" + SEPERATOR + "large.txt", session, roots);

        TestUtility.compareDiskAndView(fileRoot2, fileRoot1, session);

        TestUtility.copyFile("childDir2" + SEPERATOR + "small.txt", "childDir2" + SEPERATOR + "smallRegenerated.txt", session, roots);
        TestUtility.copyFile("childDir2" + SEPERATOR + "medium.txt", "childDir2" + SEPERATOR + "mediumRegenerated.txt", session, roots);
        TestUtility.copyFile("childDir2" + SEPERATOR + "large.txt", "childDir2" + SEPERATOR + "largeRegenerated.txt", session, roots);

        TestUtility.deleteFile("childDir2" + SEPERATOR + "small.txt", session, roots);
        TestUtility.deleteFile("childDir2" + SEPERATOR + "medium.txt", session, roots);
        TestUtility.deleteFile("childDir2" + SEPERATOR + "large.txt", session, roots);

        TestUtility.moveFile("childDir2" + SEPERATOR + "smallRegenerated.txt", "childDir1" + SEPERATOR + "small.txt", session, roots);
        TestUtility.moveFile("childDir2" + SEPERATOR + "mediumRegenerated.txt", "childDir1" + SEPERATOR + "medium.txt", session, roots);
        TestUtility.moveFile("childDir2" + SEPERATOR + "largeRegenerated.txt", "childDir1" + SEPERATOR + "large.txt", session, roots);

        TestUtility.compareDiskAndView(fileRoot2, fileRoot1, session);

        TestUtility.truncateFile("childDir1" + SEPERATOR + "small.txt", smallFileSize / 2, session, roots);
        TestUtility.truncateFile("childDir1" + SEPERATOR + "medium.txt", mediumFileSize / 2, session, roots);
        TestUtility.truncateFile("childDir1" + SEPERATOR + "large.txt", largeFileSize / 2, session, roots);

        TestUtility.writeDataToOutputStreams(chant, smallFileSize / 2, "childDir1" + SEPERATOR + "small.txt", session, roots);
        TestUtility.writeDataToOutputStreams(chant, mediumFileSize / 2, "childDir1" + SEPERATOR + "medium.txt", session, roots);
        TestUtility.writeDataToOutputStreams(chant, largeFileSize / 2, "childDir1" + SEPERATOR + "large.txt", session, roots);

        TestUtility.moveFile("childDir1", "childDir2" + SEPERATOR + "childDir1", session, roots);
        TestUtility.createFile("childDir2" + SEPERATOR + "childDir1" + SEPERATOR + "grandChild11", true, session, roots);
        TestUtility.createFile("childDir2" + SEPERATOR + "childDir1" + SEPERATOR + "grandChild11" + SEPERATOR + "grandgrandChild111", false, session, roots);
        TestUtility.moveFile("childDir2" + SEPERATOR + "childDir1" + SEPERATOR + "grandChild11", "childDir2" + SEPERATOR + "grandChild11", session, roots);
        TestUtility.moveFile("childDir2" + SEPERATOR + "childDir1", "childDir1", session, roots);
        TestUtility.moveFile("childDir2" + SEPERATOR + "grandChild11", "childDir1" + SEPERATOR + "grandChild11", session, roots);

        TestUtility.createFile("childDir1" + SEPERATOR + "dirToDelete", true, session, roots);
        TestUtility.createFile("childDir1" + SEPERATOR + "dirToDelete" + SEPERATOR + "delete.txt", false, session, roots);
        TestUtility.moveFile("childDir1" + SEPERATOR + "dirToDelete", "childDir2" + SEPERATOR + "dirToDeleteMoved", session, roots);
        TestUtility.deleteFile("childDir2" + SEPERATOR + "dirToDeleteMoved" + SEPERATOR + "delete.txt", session, roots);
        TestUtility.moveFile("childDir2" + SEPERATOR + "dirToDeleteMoved", "childDir1" + SEPERATOR + "dirToDelete", session, roots);
        TestUtility.deleteFile("childDir1" + SEPERATOR + "dirToDelete", session, roots);

        TestUtility.compareDiskAndView(fileRoot2, fileRoot1, session);

        session.commit(true);
        TestUtility.compareDiskAndDisk(fileRoot2, fileRoot1);
    }

    public static void testIOOperationsPostCrash(String testDirectory) throws Exception {
        String roots[] = new String[2];
        roots[0] = testDirectory + SEPERATOR + "dir1";
        roots[1] = testDirectory + SEPERATOR + "dir2";
        File fileRoot1 = new File(roots[0]);
        File fileRoot2 = new File(roots[1]);
        if (fileRoot1.exists() && fileRoot2.exists() && fileRoot1.listFiles().length > 0) {
            System.out.println("Assuming it was Commit before crash.");
            TestUtility.compareDiskAndDisk(fileRoot1, fileRoot2);
        } else {
            System.out.println("Assuming it was NO Commit before crash.");
        }
    }

    public static void testDynamicReadWrite(String testDirectory, int initialFileSize) throws Exception {
        String roots[] = new String[2];
        roots[0] = testDirectory + SEPERATOR + "dir1";
        roots[1] = testDirectory + SEPERATOR + "dir2";
        File fileRoot1 = new File(roots[0]);
        File fileRoot2 = new File(roots[1]);
        TestUtility.cleanupDirectory(fileRoot1);
        TestUtility.cleanupDirectory(fileRoot2);
        fileRoot1.mkdirs();
        fileRoot2.mkdirs();

        byte modulo = 111;
        for (int i = 0; i < 2; i++) {
            File source = new File(roots[i] + SEPERATOR + "source.txt");
            FileOutputStream fos = new FileOutputStream(source);
            for (int j = 0; j < initialFileSize; j++) {
                fos.write(j % modulo);
            }
            fos.flush();
            fos.close();
        }
        File source1 = new File(roots[0] + SEPERATOR + "source.txt");
        File source2 = new File(roots[1] + SEPERATOR + "source.txt");

        Session session = XAFileSystem.getXAFileSystem().createSessionForLocalTransaction();

        XAFileInputStream xisSource1 = session.createXAFileInputStream(source1, false);
        XAFileOutputStream xosSource1 = session.createXAFileOutputStream(source1, false);
        FileOutputStream fosSource2 = new FileOutputStream(source2, true);
        FileChannel fcSource2 = fosSource2.getChannel();

        int currentViewFileSize = initialFileSize;
        Random randomPosition = new Random();
        long appendAmountInEachRound = initialFileSize / 5;
        int totalRounds = 100;
        for (int rounds = 1; rounds < totalRounds; rounds++) {
            if (rounds == totalRounds / 2) {
                xosSource1.close();
                xosSource1 = session.createXAFileOutputStream(source1, true);
            }
            if (rounds % (totalRounds / 20 + 1) == 0) {
                xosSource1.close();
                int sizeToTruncateTo = randomPosition.nextInt(currentViewFileSize - 1);
                session.truncateFile(source1, sizeToTruncateTo);
                fcSource2.truncate(sizeToTruncateTo);
                xosSource1 = session.createXAFileOutputStream(source1, false);
                currentViewFileSize = sizeToTruncateTo;
            }
            for (int i = currentViewFileSize; i < currentViewFileSize + appendAmountInEachRound; i++) {
                xosSource1.write(i % modulo);
                fosSource2.write(i % modulo);
            }
            currentViewFileSize += appendAmountInEachRound;
            xosSource1.flush();

            for (int i = 0; i < currentViewFileSize / 1000000; i++) {
                int position = randomPosition.nextInt(currentViewFileSize - 1);
                xisSource1.position(position);
                int readByte = xisSource1.read();
                if (readByte != position % modulo) {
                    throw new Exception("Testcase testDynamicReadWrite failed for position : " + position);
                }
            }
        }
        fosSource2.flush();
        session.commit(true);

        TestUtility.compareDiskAndDisk(fileRoot2, fileRoot1);
    }

    public static void testDynamicReadWritePostCrash(String testDirectory, int initialFileSize) throws Exception {
        String roots[] = new String[2];
        roots[0] = testDirectory + SEPERATOR + "dir1";
        roots[1] = testDirectory + SEPERATOR + "dir2";
        File source1 = new File(roots[0] + SEPERATOR + "source.txt");
        File source2 = new File(roots[1] + SEPERATOR + "source.txt");
        if (source1.exists() && source2.exists() && source1.length() != initialFileSize) {
            System.out.println("Assuming it was Commit before crash.");
            TestUtility.compareDiskAndDisk(source1, source2);
        } else {
            System.out.println("Assuming it was NO Commit before crash.");
            byte modulo = 111;
            if (source1.exists()) {
                long length = source1.length();
                if (length != initialFileSize) {
                    throw new AssertionFailedException("File Content-Length Mismatch: " + source1);
                }
                FileInputStream fis1 = new FileInputStream(source1);
                for (int i = 0; i < length; i++) {
                    int b = fis1.read();
                    if (b != i % modulo) {
                        throw new AssertionFailedException("File Content Mismatch: " + source1 + " at " +
                                "position " + i);
                    }
                }
            }
        }
    }

    public static void testConcurrentMoneyTransfer(String testDirectory, final boolean bePessimistic)
            throws Exception {
        final File rich = new File(testDirectory + SEPERATOR + "rich.txt");
        final File poor = new File(testDirectory + SEPERATOR + "poor.txt");
        TestUtility.cleanupDirectory(new File(testDirectory));
        new File(testDirectory).mkdirs();
        FileIOUtility.createFile(rich);
        FileIOUtility.createFile(poor);
        FileOutputStream fos = new FileOutputStream(rich);
        fos.write(100);
        fos.close();
        fos = new FileOutputStream(poor);
        fos.write(0);
        fos.close();

        ArrayList<Thread> allThreads = new ArrayList<Thread>();

        for (int round = 0; round < 120; round++) {
            int cityCharityLevel = 3;
            for (int i = 0; i < cityCharityLevel; i++) {
                Thread accountant = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            plainOldMoneyTransfer(rich, poor, XAFileSystem.getXAFileSystem().createSessionForLocalTransaction(),
                                    bePessimistic);
                            System.out.println("Suceess.");
                        } catch (Exception e) {
                            System.out.println("Failure due to :" + e.getMessage());
                        }
                    }
                });
                accountant.start();
                allThreads.add(accountant);
            }
            TestUtility.waitForAllAtHeaven(allThreads);
        }
        verifyMoneyTransfersPostAllCommit(rich, poor);
    }

    private static void plainOldMoneyTransfer(File rich, File poor, Session session,
            boolean bePessimistic) throws Exception {

        XAFileInputStream xisPoor = session.createXAFileInputStream(poor, bePessimistic);
        XAFileInputStream xisRich = session.createXAFileInputStream(rich, bePessimistic);

        long poorLength = session.getFileLength(poor, bePessimistic);
        long richLength = session.getFileLength(rich, bePessimistic);

        xisPoor.position(poorLength - 1);
        byte poorHas = (byte) xisPoor.read();
        xisRich.position(richLength - 1);
        byte richHas = (byte) xisRich.read();
        XAFileOutputStream xosPoor = session.createXAFileOutputStream(poor, false);
        XAFileOutputStream xosRich = session.createXAFileOutputStream(rich, false);
        if (richHas > 0) {
            richHas--;
            poorHas++;
            xosPoor.write(poorHas);
            xosRich.write(richHas);
        }
        xisPoor.close();
        xisRich.close();
        xosPoor.close();
        xosRich.close();

        session.commit(true);
    }

    private static void verifyMoneyTransfersPostAllCommit(File rich, File poor) throws Exception {
        FileInputStream fisRich = new FileInputStream(rich);
        for (int i = 0; i < fisRich.getChannel().size(); i++) {
            if (fisRich.read() != 100 - i) {
                throw new AssertionFailedException("Content Unexpected@" + i + ": " + rich);
            }
        }
        FileInputStream fisPoor = new FileInputStream(poor);
        for (int i = 0; i < fisPoor.getChannel().size(); i++) {
            if (fisPoor.read() != i) {
                throw new AssertionFailedException("Content Unexpected@" + i + ": " + poor);
            }
        }
        if (fisRich.getChannel().size() != fisPoor.getChannel().size()) {
            throw new AssertionFailedException("Content Length Mismtach among poor and rich");
        }
    }

    public static void testConcurrentMoneyTransferPostCrash(String testDirectory)
            throws Exception {
        File rich = new File(testDirectory + SEPERATOR + "rich.txt");
        File poor = new File(testDirectory + SEPERATOR + "poor.txt");
        if (rich.exists() && poor.exists() && rich.length() + poor.length() > 2) {
            verifyMoneyTransfersPostAllCommit(rich, poor);
        }
    }

    public static void testFileSystemEventing(String testDirectory) throws Exception {
        File testDirectoryFile = new File(testDirectory);
        TestUtility.cleanupDirectory(testDirectoryFile);
        testDirectoryFile.mkdirs();

        File interestingFiles[] = new File[2];
        interestingFiles[0] = new File(testDirectory + SEPERATOR + "polledFile1.txt");
        interestingFiles[1] = new File(testDirectory + SEPERATOR + "polledFile2.txt");
        ActivationSpecImpl actSpec = new ActivationSpecImpl();
        String fileNamesAndInterests = interestingFiles[0].getAbsolutePath() + "::111" + "," +
                interestingFiles[1].getAbsolutePath() + "::111";
        actSpec.setFileNamesAndEventInterests(fileNamesAndInterests);
        SimulatedMessageEndpointFactory smef = new SimulatedMessageEndpointFactory();
        EndPointActivation epActivation = new EndPointActivation(smef, actSpec);
        XAFileSystem.getXAFileSystem().getFileSystemEventDelegator().registerActivation(epActivation);

        Session session = XAFileSystem.getXAFileSystem().createSessionForLocalTransaction();
        session.createFile(interestingFiles[0], false);
        session.copyFile(interestingFiles[0], interestingFiles[1]);
        session.deleteFile(interestingFiles[0]);
        session.commit(true);

        while(smef.getEventsReceivedCount() < 4) {
            Thread.sleep(10000);
            System.out.println("Not all events recevied yet.");
        }
    }

    public static void testFileSystemEventingPostCrash(String testDirectory) throws Exception {
        return;
    }
}

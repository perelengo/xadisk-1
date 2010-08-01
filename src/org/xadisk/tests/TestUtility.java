package org.xadisk.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Random;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.filesystem.utilities.FileIOUtility;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.bridge.proxies.impl.RemoteXAFileSystem;

public class TestUtility {

    public static boolean remoteXAFileSystem = false;

    private static Object namesake = new TestUtility();
    
    public static XAFileSystem getXAFileSystemForTest() {
        if(remoteXAFileSystem) {
            try {
                return new RemoteXAFileSystem("localhost", 9999);
            } catch(Throwable t) {
                t.printStackTrace();
                return null;
            }
        } else {
            return NativeXAFileSystem.getXAFileSystem();
        }
    }

    public static void compareDiskAndDisk(File file1, File file2) throws AssertionFailedException {
        try {
            if (file1.isDirectory() != file2.isDirectory()) {
                throw new AssertionFailedException("Directory Existence Mismatch: " + file1);
            }
            if (file1.isDirectory()) {
                File filesIn1[] = file1.listFiles();
                if (filesIn1.length != file2.listFiles().length) {
                    throw new AssertionFailedException("Directory List-Size Mismatch: " + file1);
                }
                for (int i = 0; i < filesIn1.length; i++) {
                    File fileIn2 = new File(file2.getAbsolutePath(), filesIn1[i].getName());
                    compareDiskAndDisk(filesIn1[i], fileIn2);
                }
            } else {
                FileChannel fc1 = new FileInputStream(file1).getChannel();
                FileChannel fc2 = new FileInputStream(file2).getChannel();
                ByteBuffer buffer1 = ByteBuffer.allocate(4000);
                ByteBuffer buffer2 = ByteBuffer.allocate(4000);
                if (fc1.size() != fc2.size()) {
                    throw new AssertionFailedException("File Content-Length Mismatch: " + file1);
                }
                while (true) {
                    buffer1.clear();
                    buffer2.clear();
                    int count = fc1.read(buffer1);
                    if (count == -1) {
                        break;
                    }
                    int num2 = 0;
                    while (num2 < count) {
                        num2 += fc2.read(buffer2);
                    }
                    buffer1.flip();
                    buffer2.flip();
                    for (int i = 0; i < buffer1.limit(); i++) {
                        if (buffer1.get(i) != buffer2.get(i)) {
                            throw new AssertionFailedException("File Content Mismatch@" + fc2.position() +
                                    i + ": " + file1);
                        }
                    }
                }
                fc1.close();
                fc2.close();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw new AssertionFailedException(t);
        }
    }

    public static void compareDiskAndView(File diskFile, File viewFile, Session session) throws
            AssertionFailedException {
        try {
            if (!diskFile.exists() || !session.fileExists(viewFile, false)) {
                throw new AssertionFailedException("File Existence-Check Mismatch: " + diskFile);
            }
            if (diskFile.isDirectory() != session.fileExistsAndIsDirectory(viewFile, false)) {
                throw new AssertionFailedException("Directory Existence Mismatch: " + diskFile);
            }
            if (diskFile.isDirectory()) {
                File filesInDiskFile[] = diskFile.listFiles();
                if (filesInDiskFile.length != session.listFiles(viewFile, false).length) {
                    throw new AssertionFailedException("Directory List-Size Mismatch: " + diskFile);
                }
                for (int i = 0; i < filesInDiskFile.length; i++) {
                    File fileInViewFile = new File(viewFile.getAbsolutePath(), filesInDiskFile[i].getName());
                    compareDiskAndView(filesInDiskFile[i], fileInViewFile, session);
                }
            } else {
                RandomAccessFile raDiskFile = new RandomAccessFile(diskFile, "r");
                XAFileInputStream xisViewFile = session.createXAFileInputStream(viewFile, false);
                if (raDiskFile.length() != session.getFileLength(viewFile, false)) {
                    throw new AssertionFailedException("File Content-Length Mismatch: " + diskFile);
                }
                Random randomPosition = new Random();
                for (int i = 0; i < raDiskFile.length() / 100; i++) {
                    long position = randomPosition.nextInt((int) raDiskFile.length() - 1);
                    xisViewFile.position(position);
                    raDiskFile.seek(position);
                    byte a = (byte) xisViewFile.read();
                    byte b = (byte) raDiskFile.readByte();
                    if (a != b) {
                        throw new AssertionFailedException("File Content Mismatch@" + position + ": " + diskFile);
                    }
                }
                raDiskFile.close();
                xisViewFile.close();
            }
        } catch (Throwable t) {
            throw new AssertionFailedException(t);
        }
    }

    public static void cleanupDirectory(File dir) throws IOException {
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                if (FileIOUtility.listDirectoryContents(files[i]).length > 0) {
                    cleanupDirectory(files[i]);
                    FileIOUtility.deleteFile(files[i]);
                } else {
                    FileIOUtility.deleteFile(files[i]);
                }
            } else {
                FileIOUtility.deleteFile(files[i]);
            }
        }
    }

    public static void writeDataToOutputStreams(byte data[], long approxWriteSize, String relativePath,
            Session session, String roots[]) throws Exception {
        boolean useHeavyWrite = approxWriteSize > 100000;
        XAFileOutputStream xafos = session.createXAFileOutputStream(getFileInRoot(roots[0], relativePath), useHeavyWrite);
        FileOutputStream fos = new FileOutputStream(getFileInRoot(roots[1], relativePath), true);
        for (int i = 0; i < approxWriteSize / data.length; i++) {
            fos.write(data);
            xafos.write(data);
        }
        fos.close();
        xafos.close();
    }

    public static void createFile(String relativePath, boolean isDirectory, Session session, String roots[]) throws Exception {
        session.createFile(getFileInRoot(roots[0], relativePath), isDirectory);
        if (isDirectory) {
            FileIOUtility.createDirectory(getFileInRoot(roots[1], relativePath));
        } else {
            FileIOUtility.createFile(getFileInRoot(roots[1], relativePath));
        }
    }

    public static void deleteFile(String relativePath, Session session, String roots[]) throws Exception {
        session.deleteFile(getFileInRoot(roots[0], relativePath));
        FileIOUtility.deleteFile(getFileInRoot(roots[1], relativePath));
    }

    public static void copyFile(String relativePathSrc, String relativePathDest, Session session, String roots[]) throws Exception {
        session.copyFile(getFileInRoot(roots[0], relativePathSrc), getFileInRoot(roots[0], relativePathDest));
        File src2 = getFileInRoot(roots[1], relativePathSrc);
        File dest2 = getFileInRoot(roots[1], relativePathDest);
        FileChannel fcSrc2 = new FileInputStream(src2).getChannel();
        FileChannel fcDest2 = new FileOutputStream(dest2).getChannel();
        long num = 0;
        while (num < fcSrc2.size()) {
            num += fcDest2.transferFrom(fcSrc2, fcDest2.position(), fcSrc2.size() - num);
        }

        fcSrc2.close();
        fcDest2.close();
    }

    public static void moveFile(String relativePathSrc, String relativePathDest, Session session, String roots[]) throws Exception {
        session.moveFile(getFileInRoot(roots[0], relativePathSrc), getFileInRoot(roots[0], relativePathDest));
        FileIOUtility.renameTo(getFileInRoot(roots[1], relativePathSrc), getFileInRoot(roots[1], relativePathDest));
    }

    public static void truncateFile(String relativePath, long newLength, Session session, String roots[]) throws Exception {
        session.truncateFile(getFileInRoot(roots[0], relativePath), newLength);
        FileChannel fc = new FileOutputStream(getFileInRoot(roots[1], relativePath), true).getChannel();
        fc.truncate(newLength);
        fc.close();
    }

    public static File getFileInRoot(
            String root, String relativePath) {
        return new File(root + File.separator + relativePath);
    }
    
    public static void waitForAllAtHeaven(ArrayList<Thread> threads) {
        for (Thread t : threads) {
            while (t.isAlive()) {
                try {
                    t.join();
                } catch (InterruptedException ie) {
                }
            }
        }
    }
}
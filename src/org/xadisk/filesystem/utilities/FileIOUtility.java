/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */


package org.xadisk.filesystem.utilities;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileIOUtility {

    public static void renameTo(File src, File dest) throws IOException {
        if (!src.renameTo(dest)) {
            if (renamePossible(src, dest)) {
                boolean interrupted = false;
                while (!src.renameTo(dest)) {
                    interrupted |= makeSpaceForGC();
                    if (src.renameTo(dest)) {
                        break;
                    }
                    if (!src.isDirectory()) {
                        FileChannel srcFC = new FileInputStream(src).getChannel();
                        FileChannel destFC = new FileOutputStream(dest).getChannel();
                        long size = srcFC.size();
                        long num = 0;
                        while (num < size) {
                            num += destFC.transferFrom(srcFC, num, size - num);
                        }
                        srcFC.close();
                        destFC.close();
                        if (src.delete()) {
                            break;
                        }
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            } else {
                throw new IOException("Rename not feasible from " + src + " to " + dest);
            }
        }
    }

    private static boolean renamePossible(File src, File dest) {
        return src.exists() && !dest.exists() && src.getParentFile().canWrite() && dest.getParentFile().canWrite();
    }

    public static void deleteFile(File f) throws IOException {
        if (f.delete()) {
            return;
        }
        if (!f.getParentFile().canWrite()) {
            throw new IOException("Parent directory not writable.");
        }
        if (!f.exists()) {
            throw new IOException("File does not exist.");
        }
        boolean interrupted = false;
        while (!f.delete()) {
            interrupted |= makeSpaceForGC();
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void deleteEmptyDirectory(File dir) throws IOException {
        deleteFile(dir);
    }

    public static void deleteDirectoryRecursively(File dir) throws IOException {
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                deleteDirectoryRecursively(files[i]);
            } else {
                deleteFile(files[i]);
            }
        }
        deleteEmptyDirectory(dir);
    }

    public static void createFile(File f) throws IOException {
        if (f.createNewFile()) {
            return;
        }
        if (f.exists()) {
            throw new IOException("File already exists.");
        }
        if (!f.getParentFile().canWrite()) {
            throw new IOException("Parent directory not writable.");
        }
        boolean interrupted = false;
        while (!f.createNewFile()) {
            interrupted |= makeSpaceForGC();
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public static void createDirectory(File dir) throws IOException {
        if (dir.mkdir()) {
            return;
        }
        if (dir.exists()) {
            throw new IOException("File already exists.");
        }
        if (!dir.getParentFile().canWrite()) {
            throw new IOException("Parent directory not writable.");
        }
        boolean interrupted = false;
        while (!dir.mkdir()) {
            interrupted |= makeSpaceForGC();
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public static void createDirectoriesIfRequired(File dir) throws IOException {
        if (dir.isDirectory()) {
            return;
        }
        createDirectoriesIfRequired(dir.getParentFile());
        createDirectory(dir);
    }

    public static String[] listDirectoryContents(File dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IOException("The directory doesn't exist.");
        }
        if (!dir.canRead()) {
            throw new IOException("The directory is not readable.");
        }
        String children[] = dir.list();
        boolean interrupted = false;
        while (children == null) {
            interrupted |= makeSpaceForGC();
            children = dir.list();
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return children;
    }

    private static boolean makeSpaceForGC() {
        System.out.println("Bad times: need GC.");
        System.gc();
        System.gc();
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
            return true;
        }
        return false;
    }

    public static void readFromChannel(FileChannel fc, ByteBuffer buffer, int bufferOffset, int num)
            throws IOException, EOFException {
        buffer.position(bufferOffset);
        if (buffer.remaining() < num) {
            throw new BufferUnderflowException();
        }
        buffer.limit(bufferOffset + num);
        int numRead = 0;
        int t = 0;
        while (numRead < num) {
            t = fc.read(buffer);
            if (t == -1) {
                throw new EOFException();
            }
            numRead += t;
        }
    }

    public static void copyFile(File src, File dest, boolean append) throws IOException {
        FileChannel srcChannel = new FileInputStream(src).getChannel();
        FileChannel destChannel = new FileOutputStream(dest, append).getChannel();
        long contentLength = srcChannel.size();
        long num = 0;
        while (num < contentLength) {
            num += srcChannel.transferTo(num, contentLength - num, destChannel);
        }

        destChannel.force(false);
        srcChannel.close();
        destChannel.close();
    }

    public static void copyFile(InputStream srcStream, File dest, boolean append) throws IOException {
        FileOutputStream destStream = new FileOutputStream(dest, append);
        byte[] b = new byte[1000];
        int numRead = 0;
        while (true) {
            numRead = srcStream.read(b);
            if (numRead == -1) {
                break;
            }
            destStream.write(b, 0, numRead);
        }

        destStream.flush();
        destStream.close();
        srcStream.close();
    }
}

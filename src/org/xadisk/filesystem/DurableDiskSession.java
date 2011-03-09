/*
Copyright © 2010,2011 Nitin Verma (project owner for XADisk http://xadisk.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/

package org.xadisk.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.xadisk.filesystem.utilities.FileIOUtility;

public class DurableDiskSession {

    private Set<File> directoriesToForce = new HashSet<File>();
    //private Map<File, FileChannel> fileChannelsToForce = new HashMap<File, FileChannel>();

    static {
        System.loadLibrary("xadisk");
    }

    private static native boolean forceDirectories(String directoryPaths[]);

    public DurableDiskSession() {
    }

    public void forceToDisk() throws IOException {
        String paths[] = new String[directoriesToForce.size()];
        int i = 0;
        for (File dir : directoriesToForce) {
            paths[i++] = dir.getAbsolutePath();
        }
        if (!forceDirectories(paths)) {
            throw new IOException("Fatal Error: Directory changes could not be forced-to-disk during transaction commit.");
        }
        /*for (FileChannel fc : fileChannelsToForce.values()) {
            fc.force(true);
            fc.close();
        }*/
    }

    private static void forceToDisk(String directory) throws IOException {
        String paths[] = new String[1];
        paths[0] = directory;
        if (!forceDirectories(paths)) {
            throw new IOException("Fatal Error: Directory changes could not be forced-to-disk during transaction commit.");
        }
    }

    public void renameTo(File src, File dest) throws IOException {
        directoriesToForce.add(src.getParentFile());
        directoriesToForce.add(dest.getParentFile());
        directoriesToForce.remove(src);
        FileIOUtility.renameTo(src, dest);
    }

    public void deleteFile(File f) throws IOException {
        directoriesToForce.add(f.getParentFile());
        directoriesToForce.remove(f);
        FileIOUtility.deleteFile(f);
    }

    public static void deleteFileDurably(File file) throws IOException {
        FileIOUtility.deleteFile(file);
        forceToDisk(file.getParentFile().getAbsolutePath());
    }

    public void createFile(File f) throws IOException {
        directoriesToForce.add(f.getParentFile());
        FileIOUtility.createFile(f);
    }

    public static void createFileDurably(File file) throws IOException {
        FileIOUtility.createFile(file);
        forceToDisk(file.getParentFile().getAbsolutePath());
    }

    public void createDirectory(File dir) throws IOException {
        directoriesToForce.add(dir.getParentFile());
        FileIOUtility.createDirectory(dir);
    }

    public static void createDirectoryDurably(File dir) throws IOException {
        FileIOUtility.createDirectory(dir);
        forceToDisk(dir.getParentFile().getAbsolutePath());
    }

    public void deleteDirectoryRecursively(File dir) throws IOException {
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

    public void createDirectoriesIfRequired(File dir) throws IOException {
        if (dir.isDirectory()) {
            return;
        }
        createDirectoriesIfRequired(dir.getParentFile());
        createDirectory(dir);
    }

    private void deleteEmptyDirectory(File dir) throws IOException {
        deleteFile(dir);
    }
    /* -- Ok, commenting, this optimization on a separate moment.
    //be careful that this assume the file exists, and opens the channel in append mode.
    //we were able to always use this "append=true" because all of our calls
    //in nativesession were like that earlier too.
    public FileChannel getFileChannel(File filePath) throws IOException {
    //for two different append modes
    FileChannel fc = fileChannelsToForce.get(filePath);
    if (fc == null) {
    FileOutputStream fos = new FileOutputStream(filePath, true);
    fc = fos.getChannel();
    fileChannelsToForce.put(filePath, fc);
    }
    return fc;
    }
     */
}

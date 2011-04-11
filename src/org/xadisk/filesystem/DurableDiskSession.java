/*
Copyright © 2010,2011 Nitin Verma (project owner for XADisk http://xadisk.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */

package org.xadisk.filesystem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.xadisk.filesystem.exceptions.XASystemBootFailureException;
import org.xadisk.filesystem.utilities.FileIOUtility;

public class DurableDiskSession {

    private Set<File> directoriesToForce = new HashSet<File>();
    //private Map<File, FileChannel> fileChannelsToForce = new HashMap<File, FileChannel>();
    private static boolean synchronizeDirectoryChanges;
    private static File libFilePath;

    private enum OperatingSystem {

        linux, windows, others
    };

    //this would get called only once (though, there is no language level barrier as such)
    public static void setSynchronizeDirectoryChanges(boolean syncDirectoryChanges) throws IOException {
        synchronizeDirectoryChanges = syncDirectoryChanges;
        if (synchronizeDirectoryChanges) {
            String osName = System.getProperty("os.name");
            String jvmWidth = System.getProperty("os.arch");
            boolean success = false;
            String jvm32Or64 = "32";
            if (jvmWidth.contains("64")) {
                jvm32Or64 = "64";
            }
            if (osName.contains("Linux")) {
                success = loadLibrary("linux", jvm32Or64, "libxadisk.so");
            } else if (osName.contains("Windows")) {
                success = loadLibrary("windows", jvm32Or64, "xadisk.dll");
            }

            if (!success) {
                //ok, now try the ugly way. Sorry, couldn't help.
                success = loadLibrary("linux", "32", "libxadisk.so");
                if (!success) {
                    success = loadLibrary("linux", "64", "libxadisk.so");
                    if (!success) {
                        success = loadLibrary("windows", "32", "xadisk.dll");
                        if (!success) {
                            success = loadLibrary("windows", "64", "xadisk.dll");
                            if (!success) {
                                throw new XASystemBootFailureException("XADisk has failed to load its native library "
                                        + "required for directory-synchronization.\n"
                                        + "You may want to set the configuration property \"synchronizeDirectoryChanges\""
                                        + "to false; but please note that this would turn-off directory-synchronization i.e. "
                                        + "directory modifications may not get synchronized to the disk at transaction commit.\n"
                                        + "If you have any questions or think this exception is not expected, please"
                                        + "consider discussing in XADisk forums, or raising a bug with details.");
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean loadLibrary(String operatingSystem, String jvm32Or64, String nativeLibraryName)
            throws IOException {
        InputStream libInputStream = DurableDiskSession.class.getClassLoader().
                getResourceAsStream("native" + "/" + operatingSystem + "/" + jvm32Or64 + "/" + nativeLibraryName);
        libFilePath = File.createTempFile("xadisk.", ".lib");
        libFilePath.deleteOnExit();//in case xadisk is terminated abnormally; to get more guarantees for deletion.
        FileIOUtility.copyFile(libInputStream, libFilePath, false);
        try {
            System.load(libFilePath.getAbsolutePath());
            return forceDirectories(new String[0]) && true;
        } catch (Throwable t) {
            FileIOUtility.deleteFile(libFilePath);
            return false;
        }
    }

    public static void cleanUp() {
        if(libFilePath != null) {
            libFilePath.delete();//ignore the return value.
        }
    }

    private static native boolean forceDirectories(String directoryPaths[]);

    public static void main(String args[]) {
        try {
            InputStream libInputStream = DurableDiskSession.class.getClassLoader().
                getResourceAsStream("xadisk.lib");
            libFilePath = File.createTempFile("xadisk.", ".lib");
            libFilePath.deleteOnExit();//in case xadisk is terminated abnormally; to get more guarantees for deletion.
            FileIOUtility.copyFile(libInputStream, libFilePath, false);
            try {
                System.load(libFilePath.getAbsolutePath());
                boolean success = forceDirectories(new String[0]) && true;
                if(success) {
                    System.out.println("The native library is working fine.");
                } else {
                    throw new Exception("The native library method returned false.");
                }
            } catch (Throwable t) {
                FileIOUtility.deleteFile(libFilePath);
                System.out.println("There is some problem in the native library.");
                t.printStackTrace();
            }
        } catch(Exception e) {
            System.out.println("There is some problem; not neessarily in the native library, but may be in setting up.");
            e.printStackTrace();
        }
    }

    public DurableDiskSession() {
    }

    public void forceToDisk() throws IOException {
        if (!synchronizeDirectoryChanges) {
            return;
        }
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
        if (!synchronizeDirectoryChanges) {
            return;
        }
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

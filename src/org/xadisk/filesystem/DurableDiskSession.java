/*
Copyright © 2010,2011 Nitin Verma (project owner for XADisk http://xadisk.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */

package org.xadisk.filesystem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.xadisk.filesystem.utilities.FileIOUtility;

public class DurableDiskSession {

    private Set<File> directoriesToForce = new HashSet<File>();
    private boolean synchronizeDirectoryChanges;

    public DurableDiskSession(boolean synchronizeDirectoryChanges) {
        this.synchronizeDirectoryChanges = synchronizeDirectoryChanges;
    }
    
    private enum NATIVE_LIB_NAMES {
        unix_32_xadisk_lib, unix_64_xadisk_lib,
        windows_32_xadisk_lib, windows_64_xadisk_lib
    };
    
    public static boolean setupDirectorySynchronization(File xaDiskHome) throws IOException {
        boolean success = false;
        for(NATIVE_LIB_NAMES nativeLibraryName : NATIVE_LIB_NAMES.values()) {
            success = installAndLoadLibrary(nativeLibraryName.name(), xaDiskHome);
            if(success) {
                break;
            }
        }
        if(success) {
            forceDirectoryHierarchy(xaDiskHome);
        }
        return success;
    }
    
    private static boolean testDirectorySynchronizationSetup() throws IOException {
        try {
            forceDirectories(new String[0]);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean installAndLoadLibrary(String nativeLibraryName, File xaDiskHome)
            throws IOException {
        InputStream libInputStream = DurableDiskSession.class.getClassLoader().
                getResourceAsStream("native" + "/" + nativeLibraryName);
        File nativeLibraryHome = new File(xaDiskHome, "native");
        FileIOUtility.createDirectoriesIfRequired(nativeLibraryHome);
        File copiedNativeLib = new File(nativeLibraryHome, nativeLibraryName);
        if(!copiedNativeLib.exists()) {
            FileIOUtility.copyFile(libInputStream, copiedNativeLib, false);
            try {
                System.load(copiedNativeLib.getAbsolutePath());
            } catch(Throwable t) {
            }
        }
        return testDirectorySynchronizationSetup();
    }
    
    private static void forceDirectoryHierarchy(File directory) throws IOException {
        List<String> allParents = new ArrayList<String>();
        File parentDirectory = directory;
        while(parentDirectory != null) {
            allParents.add(parentDirectory.getAbsolutePath());
            parentDirectory = directory.getParentFile();
        }
        Collections.reverse(allParents);
        forceDirectories(allParents.toArray(new String[allParents.size()]));
    }
    
    private static native boolean forceDirectories(String directoryPaths[]);

    public static void testNativeLibrary() {
        try {
            InputStream libInputStream = DurableDiskSession.class.getClassLoader().
                getResourceAsStream("xadisk.lib");
            File libFilePath = File.createTempFile("xadisk.", ".lib");
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
    }

    private void forceToDisk(String directory) throws IOException {
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

    public void deleteFileDurably(File file) throws IOException {
        FileIOUtility.deleteFile(file);
        forceToDisk(file.getParentFile().getAbsolutePath());
    }

    public void createFile(File f) throws IOException {
        directoriesToForce.add(f.getParentFile());
        FileIOUtility.createFile(f);
    }

    public void createFileDurably(File file) throws IOException {
        FileIOUtility.createFile(file);
        forceToDisk(file.getParentFile().getAbsolutePath());
    }

    public void createDirectory(File dir) throws IOException {
        directoriesToForce.add(dir.getParentFile());
        FileIOUtility.createDirectory(dir);
    }

    public void createDirectoryDurably(File dir) throws IOException {
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
}

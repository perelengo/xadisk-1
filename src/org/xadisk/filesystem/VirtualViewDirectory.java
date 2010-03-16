package org.xadisk.filesystem;

import java.io.File;
import java.util.HashMap;
import org.xadisk.filesystem.exceptions.FileAlreadyExistsException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.InsufficientPermissionOnFileException;

class VirtualViewDirectory {

    private final HashMap<String, File> allFiles = new HashMap<String, File>(20);
    private final HashMap<String, File> allDirs = new HashMap<String, File>(20);
    private final File pointsToPhysicalDirectory;
    private final HashMap<String, VirtualViewFile> virtualViewFiles = new HashMap<String, VirtualViewFile>(20);
    private final TransactionVirtualView owningView;
    private File virtualDirName;
    private boolean haveReadDirContents = false;

    VirtualViewDirectory(File virtualDirName, File pointsToPhysicalDirectory, TransactionVirtualView owningView) {
        this.owningView = owningView;
        this.virtualDirName = virtualDirName;
        this.pointsToPhysicalDirectory = pointsToPhysicalDirectory;
    }

    private void readDirectoryContents() {
        if (haveReadDirContents || pointsToPhysicalDirectory == null) {
            return;
        }
        File allPhysicalFiles[] = pointsToPhysicalDirectory.listFiles();
        for (int i = 0; i < allPhysicalFiles.length; i++) {
            if (allPhysicalFiles[i].isDirectory()) {
                allDirs.put(allPhysicalFiles[i].getName(), allPhysicalFiles[i]);
            } else {
                allFiles.put(allPhysicalFiles[i].getName(), allPhysicalFiles[i]);
            }
        }
        haveReadDirContents = true;
    }

    void createFile(String fileName, boolean isDirectory)
            throws FileAlreadyExistsException, InsufficientPermissionOnFileException {
        readDirectoryContents();
        if (fileExists(fileName) || dirExists(fileName)) {
            throw new FileAlreadyExistsException();
        }
        if (!isWritable()) {
            throw new InsufficientPermissionOnFileException();
        }
        if (isDirectory) {
            allDirs.put(fileName, null);
        } else {
            allFiles.put(fileName, null);
        }
    }

    void moveDirectoryInto(String dirName, File pointsToPhysicalDir)
            throws FileAlreadyExistsException, InsufficientPermissionOnFileException {
        readDirectoryContents();
        if (fileExists(dirName) || dirExists(dirName)) {
            throw new FileAlreadyExistsException();
        }
        if (!isWritable()) {
            throw new InsufficientPermissionOnFileException();
        }
        allDirs.put(dirName, pointsToPhysicalDir);
    }

    void moveFileInto(String fileName, File pointsToPhysicalFile)
            throws FileAlreadyExistsException, InsufficientPermissionOnFileException {
        readDirectoryContents();
        if (fileExists(fileName) || dirExists(fileName)) {
            throw new FileAlreadyExistsException();
        }
        if (!isWritable()) {
            throw new InsufficientPermissionOnFileException();
        }
        allFiles.put(fileName, pointsToPhysicalFile);
    }

    void deleteFile(String fileName)
            throws FileNotExistsException, InsufficientPermissionOnFileException {
        readDirectoryContents();
        if (!fileExists(fileName)) {
            throw new FileNotExistsException();
        }
        if (!isWritable()) {
            throw new InsufficientPermissionOnFileException();
        }
        allFiles.remove(fileName);
    }

    void deleteDir(String fileName) throws FileNotExistsException, InsufficientPermissionOnFileException {
        readDirectoryContents();
        if (!dirExists(fileName)) {
            throw new FileNotExistsException();
        }
        if (!this.isWritable()) {
            throw new InsufficientPermissionOnFileException();
        }
        allDirs.remove(fileName);
    }

    boolean fileExists(String file) {
        boolean exists = allFiles.containsKey(file);
        if (haveReadDirContents) {
            return exists;
        } else {
            if (exists) {
                return true;
            }
            if (pointsToPhysicalDirectory == null) {
                return false;
            } else {
                boolean physicallyExists = new File(pointsToPhysicalDirectory, file).isFile();
                if (physicallyExists) {
                    allFiles.put(file, new File(pointsToPhysicalDirectory, file));
                }
                return physicallyExists;
            }
        }
    }

    boolean dirExists(String file) {
        boolean exists = allDirs.containsKey(file);
        if (haveReadDirContents) {
            return exists;
        } else {
            if (exists) {
                return true;
            }
            if (pointsToPhysicalDirectory == null) {
                return false;
            } else {
                boolean physicallyExists = new File(pointsToPhysicalDirectory, file).isDirectory();
                if (physicallyExists) {
                    allDirs.put(file, new File(pointsToPhysicalDirectory, file));
                }
                return physicallyExists;
            }
        }
    }

    boolean isWritable() {
        if (pointsToPhysicalDirectory != null) {
            return pointsToPhysicalDirectory.canWrite();
        }
        return true;
    }

    File pointsToPhysicalFile(String file) throws FileNotExistsException {
        if (!fileExists(file)) {
            throw new FileNotExistsException();
        }
        return allFiles.get(file);
    }

    File pointsToPhysicalDirectory(String file) throws FileNotExistsException {
        if (!dirExists(file)) {
            throw new FileNotExistsException();
        }
        return allDirs.get(file);
    }

    String[] listFilesAndDirectories() {
        readDirectoryContents();
        String files[] = allFiles.keySet().toArray(new String[0]);
        String dirs[] = allDirs.keySet().toArray(new String[0]);
        String all[] = new String[files.length + dirs.length];
        for (int i = 0; i < files.length; i++) {
            all[i] = files[i];
        }
        for (int i = files.length; i < dirs.length + files.length; i++) {
            all[i] = dirs[i - files.length];
        }
        return all;
    }

    boolean isFileWritable(String fileName) {
        if (!fileExists(fileName)) {
            return false;
        }
        File pointsToPhysical = allFiles.get(fileName);
        if (pointsToPhysical != null) {
            return pointsToPhysical.canWrite();
        }
        return true;
    }

    boolean isFileReadable(String fileName) {
        if (!fileExists(fileName)) {
            return false;
        }
        File pointsToPhysical = allFiles.get(fileName);
        if (pointsToPhysical != null) {
            return pointsToPhysical.canRead();
        }
        return true;
    }

    boolean isDirWritable(String fileName) {
        if (!dirExists(fileName)) {
            return false;
        }
        File pointsToPhysical = allDirs.get(fileName);
        if (pointsToPhysical != null) {
            return pointsToPhysical.canWrite();
        }
        return true;
    }

    boolean isDirReadable(String fileName) {
        if (!dirExists(fileName)) {
            return false;
        }
        File pointsToPhysical = allDirs.get(fileName);
        if (pointsToPhysical != null) {
            return pointsToPhysical.canRead();
        }
        return true;
    }

    File getPointsToPhysicalDirectory() {
        return pointsToPhysicalDirectory;
    }

    private boolean isVirtualFileBeingWritten(String fileName) {
        VirtualViewFile vvf = virtualViewFiles.get(fileName);
        if (vvf == null) {
            return false;
        }
        return vvf.isBeingWritten();
    }

    private boolean isVirtualFileBeingRead(String fileName) {
        VirtualViewFile vvf = virtualViewFiles.get(fileName);
        if (vvf == null) {
            return false;
        }
        return vvf.isBeingRead();
    }

    boolean isNormalFileBeingReadOrWritten(String fileName) {
        return isVirtualFileBeingRead(fileName) || isVirtualFileBeingWritten(fileName);
    }

    VirtualViewFile getVirtualViewFile(String fileName) throws FileNotExistsException {
        VirtualViewFile vvf = virtualViewFiles.get(fileName);
        if (vvf != null) {
            return vvf;
        }
        File pointingToPhysicalFile = pointsToPhysicalFile(fileName);
        File virtualFileName = new File(virtualDirName.getAbsolutePath(), fileName);
        if (pointingToPhysicalFile != null) {
            vvf = new VirtualViewFile(virtualFileName, pointingToPhysicalFile.length(), owningView);
            vvf.setMappedToThePhysicalFileTill(pointingToPhysicalFile.length());
            vvf.setMappedToPhysicalFile(pointingToPhysicalFile);
        } else {
            vvf = new VirtualViewFile(virtualFileName, 0, owningView);
            vvf.setMappedToThePhysicalFileTill(-1);
        }
        virtualViewFiles.put(fileName, vvf);
        return vvf;
    }

    VirtualViewFile removeVirtualViewFile(String filename) {
        return virtualViewFiles.remove(filename);
    }

    void addVirtualViewFile(String filename, VirtualViewFile vvf) {
        virtualViewFiles.put(filename, vvf);
    }

    void propagateMoveCall(File targetPath) {
        this.virtualDirName = targetPath;
        for (String fileName : virtualViewFiles.keySet()) {
            VirtualViewFile vvf = (VirtualViewFile) virtualViewFiles.get(fileName);
            vvf.propagatedAncestorMoveCall(new File(targetPath.getAbsolutePath(), fileName));
        }
    }
}
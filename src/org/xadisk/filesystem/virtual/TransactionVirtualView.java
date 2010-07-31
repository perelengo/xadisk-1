package org.xadisk.filesystem.virtual;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.xadisk.filesystem.NativeSession;
import org.xadisk.filesystem.XidImpl;
import org.xadisk.filesystem.exceptions.DirectoryNotEmptyException;
import org.xadisk.filesystem.exceptions.FileAlreadyExistsException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.FileUnderUseException;
import org.xadisk.filesystem.exceptions.InsufficientPermissionOnFileException;

public class TransactionVirtualView {

    private final XidImpl owningTransaction;
    private final HashSet<File> filesWithLatestViewOnDisk = new HashSet<File>(5);
    private final HashSet<VirtualViewFile> viewFilesWithLatestViewOnDisk = new HashSet<VirtualViewFile>(5);
    private boolean transactionAlreadyDeclaredHeavyWrite = false;
    private final NativeSession owningSession;
    private final HashMap<File, VirtualViewDirectory> virtualViewDirs = new HashMap<File, VirtualViewDirectory>(10);

    public TransactionVirtualView(XidImpl owningTransaction, NativeSession owningSession) {
        this.owningTransaction = owningTransaction;
        this.owningSession = owningSession;
    }

    public void createFile(File f, boolean isDirectory)
            throws FileAlreadyExistsException, FileNotExistsException, InsufficientPermissionOnFileException {
        VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
        parentVVD.createFile(f.getName(), isDirectory);
        viewFilesWithLatestViewOnDisk.remove(new VirtualViewFile(f, 0, this));
        filesWithLatestViewOnDisk.remove(f);
    }

    public void deleteFile(File f)
            throws DirectoryNotEmptyException, FileNotExistsException, FileUnderUseException,
            InsufficientPermissionOnFileException {
        VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
        if (parentVVD.isNormalFileBeingReadOrWritten(f.getName())) {
            throw new FileUnderUseException();
        }
        if (parentVVD.dirExists(f.getName())) {
            if (getVirtualViewDirectory(f).listFilesAndDirectories().length > 0) {
                throw new DirectoryNotEmptyException();
            }
            parentVVD.deleteDir(f.getName());
            virtualViewDirs.remove(f);
            return;
        }
        if (parentVVD.fileExists(f.getName())) {
            parentVVD.deleteFile(f.getName());
            VirtualViewFile vvfIfAny = parentVVD.removeVirtualViewFile(f.getName());
            if (vvfIfAny != null) {
                vvfIfAny.propagatedDeleteCall();
            }
            return;
        }
        throw new FileNotExistsException();
    }

    public boolean isNormalFileBeingReadOrWritten(File f) {
        try {
            VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
            return parentVVD.isNormalFileBeingReadOrWritten(f.getName());
        } catch (FileNotExistsException fne) {
            return false;
        }
    }

    public boolean fileExists(File f) {
        return fileExistsAndIsNormal(f) || fileExistsAndIsDirectory(f);
    }

    public boolean fileExistsAndIsNormal(File f) {
        try {
            VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
            return parentVVD.fileExists(f.getName());
        } catch (FileNotExistsException fne) {
            return false;
        }
    }

    public boolean fileExistsAndIsDirectory(File f) {
        try {
            VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
            return parentVVD.dirExists(f.getName());
        } catch (FileNotExistsException fne) {
            return false;
        }
    }

    public String[] listFiles(File dir) throws FileNotExistsException {
        VirtualViewDirectory vvd = getVirtualViewDirectory(dir);
        return vvd.listFilesAndDirectories();
    }

    boolean isDirectoryWritable(File f) {
        try {
            VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
            return parentVVD.isDirWritable(f.getName());
        } catch (FileNotExistsException fne) {
            return false;
        }
    }

    public boolean isNormalFileWritable(File f) {
        try {
            VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
            return parentVVD.isFileWritable(f.getName());
        } catch (FileNotExistsException fne) {
            return false;
        }
    }

    boolean isDirectoryReadable(File f) {
        try {
            VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
            return parentVVD.isDirReadable(f.getName());
        } catch (FileNotExistsException fne) {
            return false;
        }
    }

    public boolean isNormalFileReadable(File f) {
        try {
            VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
            return parentVVD.isFileReadable(f.getName());
        } catch (FileNotExistsException fne) {
            return false;
        }
    }

    public VirtualViewFile getVirtualViewFile(File f) throws FileNotExistsException {
        VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
        return parentVVD.getVirtualViewFile(f.getName());
    }

    //make this move atomic in itself.
    public void moveNormalFile(File src, File dest)
            throws FileAlreadyExistsException, FileNotExistsException, FileUnderUseException,
            InsufficientPermissionOnFileException {
        VirtualViewDirectory srcParentVVD = getVirtualViewDirectory(src.getParentFile());
        VirtualViewDirectory destParentVVD = getVirtualViewDirectory(dest.getParentFile());
        if (srcParentVVD.isNormalFileBeingReadOrWritten(src.getName())) {
            throw new FileUnderUseException();
        }

        File srcPointingToPhysicalFile = srcParentVVD.pointsToPhysicalFile(src.getName());
        
        VirtualViewFile vvfSource = (VirtualViewFile) srcParentVVD.removeVirtualViewFile(src.getName());
        srcParentVVD.deleteFile(src.getName());
        
        if (vvfSource != null) {
            createFile(dest, false);
            destParentVVD.addVirtualViewFile(dest.getName(), vvfSource);
            vvfSource.propagatedMoveCall(dest);
            if (vvfSource.isUsingHeavyWriteOptimization()) {
                VirtualViewFile sourceDummyVVF = new VirtualViewFile(src, -1, this);
                sourceDummyVVF.markDeleted();
                viewFilesWithLatestViewOnDisk.add(sourceDummyVVF);
                filesWithLatestViewOnDisk.add(dest);
            }
        } else {
            viewFilesWithLatestViewOnDisk.remove(new VirtualViewFile(dest, 0, this));
            filesWithLatestViewOnDisk.remove(dest);
            destParentVVD.moveFileInto(dest.getName(), srcPointingToPhysicalFile);
        }
    }

    public void moveDirectory(File src, File dest)
            throws FileAlreadyExistsException, FileNotExistsException, InsufficientPermissionOnFileException {
        VirtualViewDirectory srcParentVVD = getVirtualViewDirectory(src.getParentFile());
        VirtualViewDirectory destParentVVD = getVirtualViewDirectory(dest.getParentFile());
        if (destParentVVD.fileExists(dest.getName()) || destParentVVD.dirExists(dest.getName())) {
            throw new FileAlreadyExistsException();
        }
        if (!srcParentVVD.dirExists(src.getName())) {
            throw new FileNotExistsException();
        }
        destParentVVD.moveDirectoryInto(dest.getName(), srcParentVVD.pointsToPhysicalDirectory(src.getName()));
        srcParentVVD.deleteDir(src.getName());
        updateDescendantVVDsWithPrefix(src, dest);
        updateVVDWithPath(src, dest);
    }

    private void updateVVDWithPath(File oldPath, File newPath) {
        VirtualViewDirectory vvd = virtualViewDirs.remove(oldPath);
        if (vvd != null) {
            virtualViewDirs.put(newPath, vvd);
            vvd.propagateMoveCall(newPath);
        }
    }

    private void updateDescendantVVDsWithPrefix(File ancestorOldName, File ancestorNewName) {
        for (File dirName : virtualViewDirs.keySet()) {
            ArrayList<String> stepsToDescendToVVD = isDescedantOf(dirName, ancestorOldName);
            if (stepsToDescendToVVD != null) {
                String newPathForVVD = ancestorNewName.getAbsolutePath();
                for (int j = stepsToDescendToVVD.size() - 1; j >= 0; j--) {
                    newPathForVVD += File.separator + stepsToDescendToVVD.get(j);
                }
                updateVVDWithPath(dirName, new File(newPathForVVD));
            }
        }
    }

    private static ArrayList<String> isDescedantOf(File a, File b) {
        File parentA = a.getParentFile();
        ArrayList<String> stepsToDescend = new ArrayList<String>(10);
        stepsToDescend.add(a.getName());
        while (parentA != null) {
            if (parentA.equals(b)) {
                return stepsToDescend;
            }
            stepsToDescend.add(parentA.getName());
            parentA = parentA.getParentFile();
        }
        return null;
    }

    private VirtualViewDirectory getVirtualViewDirectory(File f) throws FileNotExistsException {
        VirtualViewDirectory vvd = virtualViewDirs.get(f);
        if (vvd != null) {
            return vvd;
        }
        File ancestor = f.getParentFile();
        File childDirectory = f;
        VirtualViewDirectory ancestorOfTruth = null;
        ArrayList<String> pathSteps = new ArrayList<String>(10);
        pathSteps.add(childDirectory.getName());
        while (ancestor != null) {
            pathSteps.add(ancestor.getName());
            ancestorOfTruth = virtualViewDirs.get(ancestor);
            if (ancestorOfTruth != null) {
                break;
            }
            childDirectory = ancestor;
            ancestor = ancestor.getParentFile();
        }
        if (ancestorOfTruth == null) {
            if (!f.isDirectory()) {
                throw new FileNotExistsException();
            }
            vvd = new VirtualViewDirectory(f, f, this);
            virtualViewDirs.put(f, vvd);
            return vvd;
        }
        if (!ancestorOfTruth.dirExists(childDirectory.getName())) {
            throw new FileNotExistsException();
        }

        File childDirectoryPhysicalPath = ancestorOfTruth.pointsToPhysicalDirectory(childDirectory.getName());
        if (childDirectoryPhysicalPath != null) {
            String physicalPathForVVD = childDirectoryPhysicalPath.getAbsolutePath();
            for (int i = pathSteps.size() - 3; i >= 0; i--) {
                physicalPathForVVD += File.separator + pathSteps.get(i);
            }
            File physicalDir = new File(physicalPathForVVD);
            if (!physicalDir.exists()) {
                throw new FileNotExistsException();
            }
            vvd = new VirtualViewDirectory(f, physicalDir, this);
        } else {
            vvd = new VirtualViewDirectory(f, null, this);
        }
        virtualViewDirs.put(f, vvd);
        return vvd;
    }

    void beingUsedInHeavyWriteMode(VirtualViewFile vvf) throws IOException {
        if (!transactionAlreadyDeclaredHeavyWrite) {
            owningSession.declareTransactionUsingUndoLogs();
            transactionAlreadyDeclaredHeavyWrite =
                    true;
        }

        viewFilesWithLatestViewOnDisk.add(vvf);
        filesWithLatestViewOnDisk.add(vvf.getFileName());
    }

    public HashSet<File> getFilesWithLatestViewOnDisk() {
        return filesWithLatestViewOnDisk;
    }

    public HashSet<VirtualViewFile> getViewFilesWithLatestViewOnDisk() {
        return viewFilesWithLatestViewOnDisk;
    }

    XidImpl getOwningTransaction() {
        return owningTransaction;
    }

    NativeSession getOwningSession() {
        return owningSession;
    }
}

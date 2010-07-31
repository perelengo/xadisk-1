package org.xadisk.connector.outbound;

import java.io.File;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.filesystem.exceptions.DirectoryNotEmptyException;
import org.xadisk.filesystem.exceptions.FileAlreadyExistsException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.FileUnderUseException;
import org.xadisk.filesystem.exceptions.InsufficientPermissionOnFileException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.NoOngoingTransactionException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;

public class ConnectionHandle {

    private volatile ManagedConnectionImpl mc;
    private final UserLocalTransaction userLocalTransaction;
    private boolean publishFileStateChangeEventsOnCommit = false;

    protected ConnectionHandle(ManagedConnectionImpl mc) {
        this.mc = mc;
        this.userLocalTransaction = new UserLocalTransaction(mc);
    }

    protected void setManagedConnection(ManagedConnectionImpl mc) {
        this.mc = mc;
    }

    public UserLocalTransaction getUserLocalTransaction() {
        return userLocalTransaction;
    }

    public XAFileInputStream createXAFileInputStream(File f, boolean lockExclusively) throws
            FileNotExistsException, InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().createXAFileInputStream(f, lockExclusively);
    }

    public XAFileOutputStream createXAFileOutputStream(File f, boolean heavyWrite) throws
            FileNotExistsException, FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().createXAFileOutputStream(f, heavyWrite);
    }

    public void createFile(File f, boolean isDirectory) throws
            FileAlreadyExistsException, FileNotExistsException, InsufficientPermissionOnFileException,
            LockingFailedException, NoOngoingTransactionException, TransactionRolledbackException,
            InterruptedException {
        mc.getSessionForCurrentWorkAssociation().createFile(f, isDirectory);
    }

    public void deleteFile(File f) throws DirectoryNotEmptyException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        mc.getSessionForCurrentWorkAssociation().deleteFile(f);
    }

    public void copyFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        mc.getSessionForCurrentWorkAssociation().copyFile(src, dest);
    }

    public void moveFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        mc.getSessionForCurrentWorkAssociation().moveFile(src, dest);
    }

    public boolean fileExists(File f, boolean lockExclusively) throws LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().fileExists(f, lockExclusively);
    }

    public boolean fileExistsAndIsDirectory(File f, boolean lockExclusively) throws LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().fileExistsAndIsDirectory(f, lockExclusively);
    }

    public String[] listFiles(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().listFiles(f, lockExclusively);
    }

    public long getFileLength(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        return mc.getSessionForCurrentWorkAssociation().getFileLength(f, lockExclusively);
    }

    public void truncateFile(File f, long newLength) throws FileNotExistsException, FileUnderUseException,
            InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        mc.getSessionForCurrentWorkAssociation().truncateFile(f, newLength);
    }

    public void close() {
        mc.connectionClosed(this);
    }

    public boolean getPublishFileStateChangeEventsOnCommit() {
        return publishFileStateChangeEventsOnCommit;
    }

    public void setPublishFileStateChangeEventsOnCommit(boolean publishFileStateChangeEventsOnCommit) {
        this.publishFileStateChangeEventsOnCommit = publishFileStateChangeEventsOnCommit;
        this.mc.setPublishFileStateChangeEventsOnCommit(publishFileStateChangeEventsOnCommit);
    }
}

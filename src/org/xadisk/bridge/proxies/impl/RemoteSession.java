package org.xadisk.bridge.proxies.impl;

import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.facilitators.RemoteObjectProxy;
import java.io.File;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.filesystem.exceptions.DirectoryNotEmptyException;
import org.xadisk.filesystem.exceptions.FileAlreadyExistsException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.FileUnderUseException;
import org.xadisk.filesystem.exceptions.InsufficientPermissionOnFileException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.NoOngoingTransactionException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;

public class RemoteSession extends RemoteObjectProxy implements Session {

    private static final long serialVersionUID = 1L;
    
    public RemoteSession(long objectId, RemoteMethodInvoker invoker) {
        super(objectId, invoker);
    }

    public RemoteXAFileInputStream createXAFileInputStream(File f, boolean lockExclusively) throws
            FileNotExistsException, InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        try {
            return (RemoteXAFileInputStream) invokeRemoteMethod("createXAFileInputStream", f, lockExclusively);
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoOngoingTransactionException note) {
            throw note;
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public RemoteXAFileOutputStream createXAFileOutputStream(File f, boolean heavyWrite) throws FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        try {
            return (RemoteXAFileOutputStream) invokeRemoteMethod("createXAFileOutputStream", f, heavyWrite);
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (FileUnderUseException fuue) {
            throw fuue;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoOngoingTransactionException note) {
            throw note;
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void copyFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        try {
            invokeRemoteMethod("copyFile", src, dest);
        } catch (FileAlreadyExistsException faee) {
            throw faee;
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoOngoingTransactionException note) {
            throw note;
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void createFile(File f, boolean isDirectory) throws FileAlreadyExistsException, FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException, NoOngoingTransactionException,
            TransactionRolledbackException, InterruptedException {
        try {
            invokeRemoteMethod("createFile", f, isDirectory);
        } catch (FileAlreadyExistsException faee) {
            throw faee;
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoOngoingTransactionException note) {
            throw note;
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void deleteFile(File f) throws DirectoryNotEmptyException, FileNotExistsException, FileUnderUseException,
            InsufficientPermissionOnFileException, LockingFailedException, NoOngoingTransactionException,
            TransactionRolledbackException, InterruptedException {
        try {
            invokeRemoteMethod("deleteFile", f);
        } catch (DirectoryNotEmptyException dnee) {
            throw dnee;
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (FileUnderUseException fuue) {
            throw fuue;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoOngoingTransactionException note) {
            throw note;
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public boolean fileExists(File f, boolean lockExclusively) throws LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        try {
            return (Boolean) invokeRemoteMethod("fileExists", f, lockExclusively);
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoOngoingTransactionException note) {
            throw note;
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public boolean fileExistsAndIsDirectory(File f, boolean lockExclusively) throws LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        try {
            return (Boolean) invokeRemoteMethod("fileExistsAndIsDirectory", f, lockExclusively);
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoOngoingTransactionException note) {
            throw note;
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public long getFileLength(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        try {
            return (Long) invokeRemoteMethod("getFileLength", f, lockExclusively);
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoOngoingTransactionException note) {
            throw note;
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public String[] listFiles(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        try {
            return (String[]) invokeRemoteMethod("listFiles", f, lockExclusively);
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoOngoingTransactionException note) {
            throw note;
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void moveFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException {
        try {
            invokeRemoteMethod("moveFile", src, dest);
        } catch (FileAlreadyExistsException faee) {
            throw faee;
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (FileUnderUseException fuue) {
            throw fuue;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoOngoingTransactionException note) {
            throw note;
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void truncateFile(File f, long newLength) throws FileNotExistsException, FileUnderUseException,
            InsufficientPermissionOnFileException, LockingFailedException, NoOngoingTransactionException,
            TransactionRolledbackException, InterruptedException {
        try {
            invokeRemoteMethod("truncateFile", f, newLength);
        } catch (FileNotExistsException fnee) {
            throw fnee;
        } catch (FileUnderUseException fuue) {
            throw fuue;
        } catch (InsufficientPermissionOnFileException ipfe) {
            throw ipfe;
        } catch (LockingFailedException lfe) {
            throw lfe;
        } catch (NoOngoingTransactionException note) {
            throw note;
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void prepare() throws TransactionRolledbackException {
        try {
            invokeRemoteMethod("prepare");
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void commit() throws TransactionRolledbackException {
        this.commit(true);
    }

    public void commit(boolean onePhase) throws TransactionRolledbackException {
        try {
            invokeRemoteMethod("commit", onePhase);
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void rollback() throws TransactionRolledbackException {
        try {
            invokeRemoteMethod("rollback");
        } catch (TransactionRolledbackException tre) {
            throw tre;
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public void setPublishFileStateChangeEventsOnCommit(boolean publish) {
        try {
            invokeRemoteMethod("setPublishFileStateChangeEventsOnCommit", publish);
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public boolean getPublishFileStateChangeEventsOnCommit() {
        try {
            return (Boolean) invokeRemoteMethod("getPublishFileStateChangeEventsOnCommit");
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public boolean setTransactionTimeout(int transactionTimeout) {
        try {
            return (Boolean) invokeRemoteMethod("setTransactionTimeout", transactionTimeout);
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }

    public int getTransactionTimeout() {
        try {
            return (Integer) invokeRemoteMethod("getTransactionTimeout");
        } catch (Throwable t) {
            throw assertExceptionHandling(t);
        }
    }
}

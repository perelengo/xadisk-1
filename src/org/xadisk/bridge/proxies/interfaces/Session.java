package org.xadisk.bridge.proxies.interfaces;

import java.io.File;
import org.xadisk.filesystem.exceptions.DirectoryNotEmptyException;
import org.xadisk.filesystem.exceptions.FileAlreadyExistsException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.FileUnderUseException;
import org.xadisk.filesystem.exceptions.InsufficientPermissionOnFileException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.NoOngoingTransactionException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;

public interface Session {

    public boolean setTransactionTimeout(int transactionTimeout);

    public void setPublishFileStateChangeEventsOnCommit(boolean publish);

    public XAFileInputStream createXAFileInputStream(File f, boolean lockExclusively) throws
            FileNotExistsException, InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    public XAFileOutputStream createXAFileOutputStream(File f, boolean heavyWrite) throws
            FileNotExistsException, FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    public void createFile(File f, boolean isDirectory) throws
            FileAlreadyExistsException, FileNotExistsException, InsufficientPermissionOnFileException,
            LockingFailedException, NoOngoingTransactionException, TransactionRolledbackException,
            InterruptedException;

    public void deleteFile(File f) throws DirectoryNotEmptyException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    public void copyFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    public void moveFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    public boolean fileExists(File f, boolean lockExclusively) throws LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    public boolean fileExistsAndIsDirectory(File f, boolean lockExclusively) throws LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    public String[] listFiles(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    public long getFileLength(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    public void truncateFile(File f, long newLength) throws FileNotExistsException, FileUnderUseException,
            InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    public void prepare() throws TransactionRolledbackException;

    public void rollback() throws TransactionRolledbackException;

    public void commit(boolean onePhase) throws TransactionRolledbackException;
}

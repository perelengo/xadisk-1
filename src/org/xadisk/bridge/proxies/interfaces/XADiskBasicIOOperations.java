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

/**
 * This interface declares the basic set of i/o operations which can be called
 * on XADisk.
 */
public interface XADiskBasicIOOperations {

    /**
     * Can be used to control whether the commit of the transaction on this object
     * (Session or Connection) should result in publishing of events to registered MDBs or not.
     * @param publish
     */
    public void setPublishFileStateChangeEventsOnCommit(boolean publish);

    /**
     * Returns the current value of "publish" : whether the commit of the transaction on this object
     * (Session or Connection) should result in publishing of events to registered MDBs or not.
     * @return
     */
    public boolean getPublishFileStateChangeEventsOnCommit();

    /**
     * Creates an input stream associated with the File "f".
     * This stream can further be wrapped by a utility class "XAFileInputStreamWrapper" to
     * get easy pluggability via standard java.io's InputStream.
     * @param f The target file from where to read.
     * @param lockExclusively If true, will acquire an exclusive (write) lock for the file.
     * @return the input stream object.
     * @throws FileNotExistsException
     * @throws InsufficientPermissionOnFileException
     * @throws LockingFailedException
     * @throws NoOngoingTransactionException
     * @throws TransactionRolledbackException
     * @throws InterruptedException
     */
    public XAFileInputStream createXAFileInputStream(File f, boolean lockExclusively) throws
            FileNotExistsException, InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    /**
     * Creates an output stream associated with the File "f". The File "f" should exist
     * from the viewpoint of the ongoing transaction (i.e. if the file didn't exist before
     * the transaction, it should be created first).
     * This stream would always append to the file. To write at an arbitrary offset of the file, this method
     * can be used along with truncateFile and the XAFileInputStream for example.
     * This stream can further be wrapped by a utility class "XAFileOutputStreamWrapper" to
     * get easy pluggability via standard java.io's OutputStream.
     * @param f The target file to which to write.
     * @param heavyWrite A clue for performance tuning. When writing a few 100 bytes set this to false.
     * @return the output stream object.
     * @throws FileNotExistsException
     * @throws FileUnderUseException
     * @throws InsufficientPermissionOnFileException
     * @throws LockingFailedException
     * @throws NoOngoingTransactionException
     * @throws TransactionRolledbackException
     * @throws InterruptedException
     */
    public XAFileOutputStream createXAFileOutputStream(File f, boolean heavyWrite) throws
            FileNotExistsException, FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    /**
     * Create a new file or directory.
     * @param f The file or directory to create.
     * @param isDirectory Tells whether to create a directory.
     * @throws FileAlreadyExistsException
     * @throws FileNotExistsException
     * @throws InsufficientPermissionOnFileException
     * @throws LockingFailedException
     * @throws NoOngoingTransactionException
     * @throws TransactionRolledbackException
     * @throws InterruptedException
     */
    public void createFile(File f, boolean isDirectory) throws
            FileAlreadyExistsException, FileNotExistsException, InsufficientPermissionOnFileException,
            LockingFailedException, NoOngoingTransactionException, TransactionRolledbackException,
            InterruptedException;

    /**
     * Deletes a file or directory.
     * @param f The file/directory to delete.
     * @throws DirectoryNotEmptyException
     * @throws FileNotExistsException
     * @throws FileUnderUseException
     * @throws InsufficientPermissionOnFileException
     * @throws LockingFailedException
     * @throws NoOngoingTransactionException
     * @throws TransactionRolledbackException
     * @throws InterruptedException
     */
    public void deleteFile(File f) throws DirectoryNotEmptyException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    /**
     * Copies a file.
     * @param src Source file.
     * @param dest Destination file.
     * @throws FileAlreadyExistsException
     * @throws FileNotExistsException
     * @throws InsufficientPermissionOnFileException
     * @throws LockingFailedException
     * @throws NoOngoingTransactionException
     * @throws TransactionRolledbackException
     * @throws InterruptedException
     */
    public void copyFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    /**
     * Moves a file or directory.
     * @param src The source path for the file or directory.
     * @param dest The destination path for the file or directory.
     * @throws FileAlreadyExistsException
     * @throws FileNotExistsException
     * @throws FileUnderUseException
     * @throws InsufficientPermissionOnFileException
     * @throws LockingFailedException
     * @throws NoOngoingTransactionException
     * @throws TransactionRolledbackException
     * @throws InterruptedException
     */
    public void moveFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    /**
     * Tells whether the file or directory exists.
     * @param f The file/directory path.
     * @param lockExclusively Set to true for obtaining an exclusive lock over the file or
     * directory. False will only obtain a shared lock.
     * @return true if the file/directory exists.
     * @throws LockingFailedException
     * @throws NoOngoingTransactionException
     * @throws TransactionRolledbackException
     * @throws InterruptedException
     */
    public boolean fileExists(File f, boolean lockExclusively) throws LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    /**
     * Tells whether the directory exists.
     * @param f The directory path.
     * @param lockExclusively Set to true for obtaining an exclusive lock over the
     * directory. False will only obtain a shared lock.
     * @return true if the directory exists.
     * @throws LockingFailedException
     * @throws NoOngoingTransactionException
     * @throws TransactionRolledbackException
     * @throws InterruptedException
     */
    public boolean fileExistsAndIsDirectory(File f, boolean lockExclusively) throws LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    /**
     * Lists the contents of the directory.
     * @param f The directory path.
     * @param lockExclusively  Set to true for obtaining an exclusive lock over the
     * directory. False will only obtain a shared lock.
     * @return An array of Strings containing names of files/directories.
     * @throws FileNotExistsException
     * @throws LockingFailedException
     * @throws NoOngoingTransactionException
     * @throws TransactionRolledbackException
     * @throws InterruptedException
     */
    public String[] listFiles(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    /**
     * Gets the length of the file.
     * @param f The file path.
     * @param lockExclusively Set to true for obtaining an exclusive lock over the
     * file. False will only obtain a shared lock.
     * @return Length of the file in bytes.
     * @throws FileNotExistsException
     * @throws LockingFailedException
     * @throws NoOngoingTransactionException
     * @throws TransactionRolledbackException
     * @throws InterruptedException
     */
    public long getFileLength(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;

    /**
     * Truncates a file.
     * @param f The file path.
     * @param newLength New length to truncate to.
     * @throws FileNotExistsException
     * @throws FileUnderUseException
     * @throws InsufficientPermissionOnFileException
     * @throws LockingFailedException
     * @throws NoOngoingTransactionException
     * @throws TransactionRolledbackException
     * @throws InterruptedException
     */
    public void truncateFile(File f, long newLength) throws FileNotExistsException, FileUnderUseException,
            InsufficientPermissionOnFileException, LockingFailedException,
            NoOngoingTransactionException, TransactionRolledbackException, InterruptedException;
}

package org.xadisk.filesystem;

import org.xadisk.filesystem.pools.PooledBuffer;
import org.xadisk.filesystem.utilities.FileIOUtility;
import org.xadisk.filesystem.virtual.TransactionVirtualView;
import org.xadisk.filesystem.virtual.NativeXAFileOutputStream;
import org.xadisk.filesystem.virtual.NativeXAFileInputStream;
import org.xadisk.filesystem.virtual.VirtualViewFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;
import org.xadisk.filesystem.exceptions.DirectoryNotEmptyException;
import org.xadisk.filesystem.exceptions.FileAlreadyExistsException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.FileUnderUseException;
import org.xadisk.filesystem.exceptions.InsufficientPermissionOnFileException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;
import org.xadisk.filesystem.exceptions.XASystemException;

public class NativeSession implements Session {

    private static final int WRITE_FILE = 0;
    private static final int READ_FILE = 1;
    private final HashMap<File, Lock> allAcquiredLocks = new HashMap<File, Lock>(1000);
    private final ArrayList<NativeXAFileInputStream> allAcquiredInputStreams = new ArrayList<NativeXAFileInputStream>(5);
    private final ArrayList<NativeXAFileOutputStream> allAcquiredOutputStreams = new ArrayList<NativeXAFileOutputStream>(5);
    private final NativeXAFileSystem xaFileSystem;
    private volatile int transactionTimeout = 0;
    private final XidImpl xid;
    private boolean rolledback = false;
    private volatile boolean startedCommitting = false;
    private Throwable rollbackCause = null;
    private volatile boolean systemHasFailed = false;
    private volatile Throwable systemFailureCause = null;
    private final TransactionVirtualView view;
    private long fileLockWaitTimeout = 200;
    private final ResourceDependencyGraph RDG;
    private boolean createdForRecovery = false;
    private ArrayList<FileStateChangeEvent> fileStateChangeEventsToRaise = new ArrayList<FileStateChangeEvent>(10);
    private final ArrayList<File> directoriesPinnedInThisSession = new ArrayList<File>(5);
    private final long timeOfEntryToTransaction;
    private final ReentrantLock asynchronousRollbackLock = new ReentrantLock(false);
    private final ArrayList<Long> transactionLogPositions = new ArrayList<Long>(25);
    private final ArrayList<Buffer> transactionInMemoryBuffers = new ArrayList<Buffer>(25);
    private int numOwnedExclusiveLocks = 0;
    private boolean publishFileStateChangeEventsOnCommit = false;

    NativeSession(XidImpl xid, boolean createdForRecovery) {
        this.xid = xid;
        xid.setOwningSession(this);
        this.xaFileSystem = NativeXAFileSystem.getXAFileSystem();
        this.RDG = xaFileSystem.getResourceDependencyGraph();
        this.createdForRecovery = createdForRecovery;
        if (createdForRecovery) {
            this.transactionTimeout = 0;
            this.view = null;
            this.timeOfEntryToTransaction = -100;
        } else {
            this.transactionTimeout = xaFileSystem.getDefaultTransactionTimeout();
            this.fileLockWaitTimeout = this.xaFileSystem.getLockTimeOut();
            view = new TransactionVirtualView(xid, this);
            RDG.createNodeForTransaction(xid);
            timeOfEntryToTransaction = System.currentTimeMillis();
            xaFileSystem.assignSessionToTransaction(xid, this);
        }
    }

    public NativeSession(XidImpl xid, ArrayList<FileStateChangeEvent> events) {
        this.xid = xid;
        xid.setOwningSession(this);
        this.xaFileSystem = NativeXAFileSystem.getXAFileSystem();
        this.RDG = xaFileSystem.getResourceDependencyGraph();
        this.createdForRecovery = true;
        this.transactionTimeout = 0;
        this.view = null;
        this.timeOfEntryToTransaction = -100;
        this.fileStateChangeEventsToRaise = events;
        this.publishFileStateChangeEventsOnCommit = true;
    }

    public void rollbackAsynchronously(Throwable rollbackCause) {
        try {
            asynchronousRollbackLock.lock();
            if (!startedCommitting) {
                rollbackPrematurely(rollbackCause);
            }
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    void rollbackPrematurely(Throwable rollbackCause) {
        try {
            rollback();
            this.rolledback = true;
            this.rollbackCause = rollbackCause;
        } catch (TransactionRolledbackException trbe) {
        }
    }

    void notifySystemFailure(Throwable systemFailureCause) {
        this.systemHasFailed = true;
        this.systemFailureCause = systemFailureCause;
    }

    public NativeXAFileInputStream createXAFileInputStream(File f, boolean lockExclusively)
            throws FileNotExistsException, InsufficientPermissionOnFileException, LockingFailedException,
            TransactionRolledbackException, InterruptedException {
        f = f.getAbsoluteFile();
        Lock newLock = null;
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLock = acquireLockIfRequired(f, lockExclusively);
            checkPermission(READ_FILE, f);
            VirtualViewFile vvf = view.getVirtualViewFile(f);
            NativeXAFileInputStream temp = new NativeXAFileInputStream(vvf, this);
            allAcquiredInputStreams.add(temp);
            addLocks(newLock);
            success = true;
            return temp;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            if (!success) {
                releaseLocks(newLock);
            }
            asynchronousRollbackLock.unlock();
        }
    }

    public NativeXAFileOutputStream createXAFileOutputStream(File f, boolean heavyWrite) throws FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            TransactionRolledbackException, InterruptedException {
        f = f.getAbsoluteFile();
        Lock newLock = null;
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLock = acquireLockIfRequired(f, true);
            checkPermission(WRITE_FILE, f);
            VirtualViewFile vvf = view.getVirtualViewFile(f);
            NativeXAFileOutputStream temp = NativeXAFileOutputStream.getCachedXAFileOutputStream(vvf, xid, heavyWrite, this);
            addLocks(newLock);
            allAcquiredOutputStreams.add(temp);
            addToFileSystemEvents(FileStateChangeEvent.FILE_MODIFIED, f, false);
            success = true;
            return temp;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            if (!success) {
                releaseLocks(newLock);
            }
            asynchronousRollbackLock.unlock();
        }
    }

    public void createFile(File f, boolean isDirectory) throws FileAlreadyExistsException, FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException, TransactionRolledbackException,
            InterruptedException {
        f = f.getAbsoluteFile();
        Lock newLocks[] = new Lock[2];
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLocks[0] = acquireLockIfRequired(f, true);
            File parentFile = f.getParentFile();
            checkValidParent(f);
            newLocks[1] = acquireLockIfRequired(parentFile, true);
            view.createFile(f, isDirectory);
            byte operation = isDirectory ? TransactionLogEntry.DIR_CREATE : TransactionLogEntry.FILE_CREATE;
            ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, f.getAbsolutePath(),
                    operation));
            Buffer logEntry = new Buffer(logEntryBytes);
            xaFileSystem.getTheGatheringDiskWriter().submitBuffer(logEntry, xid);
            addLocks(newLocks);
            addToFileSystemEvents(FileStateChangeEvent.FILE_CREATED, f, isDirectory);
            success = true;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            if (!success) {
                releaseLocks(newLocks);
            }
            asynchronousRollbackLock.unlock();
        }
    }

    public void deleteFile(File f) throws DirectoryNotEmptyException, FileNotExistsException, FileUnderUseException,
            InsufficientPermissionOnFileException, LockingFailedException, TransactionRolledbackException,
            InterruptedException {
        f = f.getAbsoluteFile();
        Lock newLocks[] = new Lock[2];
        boolean success = false;
        boolean isDirectory = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLocks[0] = acquireLockIfRequired(f, true);
            File parentFile = f.getParentFile();
            checkValidParent(f);
            newLocks[1] = acquireLockIfRequired(parentFile, true);
            isDirectory = view.deleteFile(f);
            ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, f.getAbsolutePath(),
                    TransactionLogEntry.FILE_DELETE));
            Buffer logEntry = new Buffer(logEntryBytes);
            xaFileSystem.getTheGatheringDiskWriter().submitBuffer(logEntry, xid);
            addLocks(newLocks);
            addToFileSystemEvents(FileStateChangeEvent.FILE_DELETED, f, isDirectory);
            success = true;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            if (!success) {
                releaseLocks(newLocks);
            }
            asynchronousRollbackLock.unlock();
        }
    }

    public void moveFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            FileUnderUseException, InsufficientPermissionOnFileException, LockingFailedException,
            TransactionRolledbackException, InterruptedException {
        src = src.getAbsoluteFile();
        dest = dest.getAbsoluteFile();
        Lock newLocks[] = new Lock[4];
        boolean success = false;
        boolean isDirectoryMove = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLocks[0] = acquireLockIfRequired(src, true);
            newLocks[1] = acquireLockIfRequired(dest, true);
            File srcParentFile = src.getParentFile();
            checkValidParent(src);
            File destParentFile = dest.getParentFile();
            checkValidParent(dest);
            newLocks[2] = acquireLockIfRequired(srcParentFile, true);
            newLocks[3] = acquireLockIfRequired(destParentFile, true);

            if (view.fileExistsAndIsNormal(src)) {
                view.moveNormalFile(src, dest);
            } else if (view.fileExistsAndIsDirectory(src)) {
                isDirectoryMove = true;
                checkAnyOpenStreamToDescendantFiles(src);
                xaFileSystem.pinDirectoryForRename(src, xid);
                directoriesPinnedInThisSession.add(src);
                view.moveDirectory(src, dest);
            } else {
                throw new FileNotExistsException();
            }
            ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, src.getAbsolutePath(),
                    dest.getAbsolutePath(), TransactionLogEntry.FILE_MOVE));
            Buffer logEntry = new Buffer(logEntryBytes);
            xaFileSystem.getTheGatheringDiskWriter().submitBuffer(logEntry, xid);
            addLocks(newLocks);

            addToFileSystemEvents(new byte[]{FileStateChangeEvent.FILE_DELETED,
                        FileStateChangeEvent.FILE_CREATED, FileStateChangeEvent.FILE_MODIFIED},
                    new File[]{src, dest, dest}, isDirectoryMove);

            success = true;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            if (!success) {
                releaseLocks(newLocks);
                if(isDirectoryMove) {
                    xaFileSystem.releaseRenamePinOnDirectory(src);
                }
            }
            asynchronousRollbackLock.unlock();
        }
    }

    public void copyFile(File src, File dest) throws FileAlreadyExistsException, FileNotExistsException,
            InsufficientPermissionOnFileException, LockingFailedException, TransactionRolledbackException,
            InterruptedException {
        src = src.getAbsoluteFile();
        dest = dest.getAbsoluteFile();
        Lock newLocks[] = new Lock[3];
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLocks[0] = acquireLockIfRequired(src, false);
            newLocks[1] = acquireLockIfRequired(dest, true);
            File destParentFile = dest.getParentFile();
            checkValidParent(src);
            checkValidParent(dest);
            newLocks[2] = acquireLockIfRequired(destParentFile, true);
            checkPermission(READ_FILE, src);
            view.createFile(dest, false);
            VirtualViewFile srcFileInView = view.getVirtualViewFile(src);
            VirtualViewFile destFileInView = view.getVirtualViewFile(dest);
            srcFileInView.takeSnapshotInto(destFileInView);
            ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, src.getAbsolutePath(),
                    dest.getAbsolutePath(), TransactionLogEntry.FILE_COPY));
            Buffer logEntry = new Buffer(logEntryBytes);
            xaFileSystem.getTheGatheringDiskWriter().submitBuffer(logEntry, xid);
            addLocks(newLocks);

            addToFileSystemEvents(new byte[]{FileStateChangeEvent.FILE_CREATED, FileStateChangeEvent.FILE_MODIFIED},
                    new File[]{dest, dest}, false);

            success = true;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            if (!success) {
                releaseLocks(newLocks);
            }
            asynchronousRollbackLock.unlock();
        }
    }

    public boolean fileExists(File f, boolean lockExclusively) throws LockingFailedException,
            TransactionRolledbackException, InterruptedException {
        f = f.getAbsoluteFile();
        Lock newLock = null;
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            File parentDir = f.getParentFile();
            if (parentDir != null) {
                newLock = acquireLockIfRequired(parentDir, lockExclusively);
                addLocks(newLock);
                success = true;
                return view.fileExists(f);
            } else {
                //f is the root.
                return f.exists();//yes, we do a physical file system check here, as no one including this txn deletes a root (/, or C:\ or D:\).
            }
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            if (!success) {
                releaseLocks(newLock);
            }
            asynchronousRollbackLock.unlock();
        }
    }

    public boolean fileExistsAndIsDirectory(File f, boolean lockExclusively) throws
            LockingFailedException, TransactionRolledbackException, InterruptedException {
        f = f.getAbsoluteFile();
        Lock newLock = null;
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            File parentDir = f.getParentFile();
            if (parentDir != null) {
                newLock = acquireLockIfRequired(parentDir, lockExclusively);
                addLocks(newLock);
                success = true;
                return view.fileExistsAndIsDirectory(f);
            } else {
                //f is the root.
                return f.exists();//yes, we do a physical file system check here, as no one including this txn deletes a root (/, or C:\ or D:\).
            }
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            if (!success) {
                releaseLocks(newLock);
            }
            asynchronousRollbackLock.unlock();
        }
    }

    public String[] listFiles(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            TransactionRolledbackException, InterruptedException {
        f = f.getAbsoluteFile();
        Lock newLock = null;
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLock = acquireLockIfRequired(f, lockExclusively);
            addLocks(newLock);
            success = true;
            return view.listFiles(f);
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            if (!success) {
                releaseLocks(newLock);
            }
            asynchronousRollbackLock.unlock();
        }
    }

    public long getFileLength(File f, boolean lockExclusively) throws FileNotExistsException, LockingFailedException,
            TransactionRolledbackException, InterruptedException {
        f = f.getAbsoluteFile();
        Lock newLock = null;
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLock = acquireLockIfRequired(f, lockExclusively);
            if (!view.fileExistsAndIsNormal(f)) {
                throw new FileNotExistsException();
            }
            addLocks(newLock);
            long length = view.getVirtualViewFile(f).getLength();
            success = true;
            return length;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            if (!success) {
                releaseLocks(newLock);
            }
            asynchronousRollbackLock.unlock();
        }
    }

    public void truncateFile(File f, long newLength) throws FileNotExistsException, FileUnderUseException,
            InsufficientPermissionOnFileException, LockingFailedException, TransactionRolledbackException, InterruptedException {
        f = f.getAbsoluteFile();
        Lock newLock = null;
        boolean success = false;
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            newLock = acquireLockIfRequired(f, true);
            checkPermission(WRITE_FILE, f);
            if (view.isNormalFileBeingReadOrWritten(f)) {
                //earlier we threw an exception here.
            }
            if (!view.fileExistsAndIsNormal(f)) {
                throw new FileNotExistsException();
            }
            VirtualViewFile vvf = view.getVirtualViewFile(f);
            vvf.truncate(newLength);
            ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, f.getAbsolutePath(), newLength,
                    TransactionLogEntry.FILE_TRUNCATE));
            Buffer logEntry = new Buffer(logEntryBytes);
            xaFileSystem.getTheGatheringDiskWriter().submitBuffer(logEntry, xid);
            addLocks(newLock);

            addToFileSystemEvents(FileStateChangeEvent.FILE_MODIFIED, f, false);

            success = true;
        } catch (XASystemException xase) {
            xaFileSystem.notifySystemFailure(xase);
            throw xase;
        } finally {
            if (!success) {
                releaseLocks(newLock);
            }
            asynchronousRollbackLock.unlock();
        }
    }

    private void submitPreCommitInformationForLogging() throws TransactionRolledbackException,
            IOException {
        releaseAllStreams();
        Iterator<VirtualViewFile> vvfsUpdatedDirectly = view.getViewFilesWithLatestViewOnDisk().iterator();
        while (vvfsUpdatedDirectly.hasNext()) {
            vvfsUpdatedDirectly.next().forceAndFreePhysicalChannel();
        }
        HashSet<File> filesOnDisk = view.getFilesWithLatestViewOnDisk();
        ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, filesOnDisk));
        xaFileSystem.getTheGatheringDiskWriter().submitBuffer(new Buffer(logEntryBytes), xid);

        if (publishFileStateChangeEventsOnCommit) {
            fileStateChangeEventsToRaise = xaFileSystem.getFileSystemEventDelegator().retainOnlyInterestingEvents(fileStateChangeEventsToRaise);
            logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, fileStateChangeEventsToRaise,
                    TransactionLogEntry.EVENT_ENQUEUE));
            xaFileSystem.getTheGatheringDiskWriter().submitBuffer(new Buffer(logEntryBytes), xid);
        }
        xaFileSystem.getTheGatheringDiskWriter().writeRemainingBuffersNow(xid);
    }

    public void prepare() throws TransactionRolledbackException {
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            submitPreCommitInformationForLogging();
            xaFileSystem.getTheGatheringDiskWriter().transactionPrepareCompletes(xid);
        } catch (TransactionRolledbackException trbe) {
            throw trbe;
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailure(ioe);
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    public void commit(boolean onePhase) throws TransactionRolledbackException {
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();
            if (onePhase) {
                try {
                    if (!createdForRecovery) {
                        submitPreCommitInformationForLogging();
                        xaFileSystem.getTheGatheringDiskWriter().transactionCommitBegins(xid);
                    }
                } catch (IOException ioe) {
                    xaFileSystem.notifySystemFailure(ioe);
                }
            }
            startedCommitting = true;
            ArrayList<Long> logPositions;
            HashSet<File> filesDirectlyWrittenToDisk;
            HashMap<Integer, FileChannel> logReaderChannels = new HashMap<Integer, FileChannel>(2);
            FileChannel logReaderChannel = null;
            String transactionLogBaseName = xaFileSystem.getTransactionLogFileBaseName();
            int latestCheckPointForRecoveryCase = 0;
            if (createdForRecovery) {
                filesDirectlyWrittenToDisk = xaFileSystem.getRecoveryWorker().getFilesOnDiskForTransaction(xid);
                logPositions = xaFileSystem.getRecoveryWorker().getTransactionLogsPositions(xid);
                latestCheckPointForRecoveryCase = xaFileSystem.getRecoveryWorker().getTransactionsLatestCheckPoint(xid);
                if (latestCheckPointForRecoveryCase == -1) {
                    latestCheckPointForRecoveryCase = 0;
                } else {
                    latestCheckPointForRecoveryCase += 2;
                }
            } else {
                filesDirectlyWrittenToDisk = view.getFilesWithLatestViewOnDisk();
                logPositions = this.transactionLogPositions;
            }
            Buffer inMemoryLog;
            for (int i = latestCheckPointForRecoveryCase; i < logPositions.size() - 1; i += 2) {
                ByteBuffer temp = null;
                int logFileIndex = (int) logPositions.get(i).longValue();
                long localPosition = logPositions.get(i + 1);
                TransactionLogEntry logEntry;
                if (logFileIndex == -1) {
                    inMemoryLog = transactionInMemoryBuffers.get((int) localPosition);
                    temp = inMemoryLog.getBuffer();
                    temp.position(0);
                    logEntry = TransactionLogEntry.parseLogEntry(temp);
                } else {
                    if (logReaderChannels.get(logFileIndex) == null) {
                        logReaderChannels.put(logFileIndex, new FileInputStream(transactionLogBaseName + "_" + logFileIndex).getChannel());
                    }
                    logReaderChannel = logReaderChannels.get(logFileIndex);
                    logReaderChannel.position(localPosition);
                    logEntry = TransactionLogEntry.getNextTransactionLogEntry(logReaderChannel, localPosition, false);
                }
                FileOutputStream fos;
                if (logEntry.getOperationType() == TransactionLogEntry.FILE_APPEND) {
                    if (filesDirectlyWrittenToDisk.contains(new File(logEntry.getFileName()))) {
                        continue;
                    }
                    commitFileAppend(logEntry, temp, logReaderChannel, logFileIndex, localPosition);
                } else if (logEntry.getOperationType() == TransactionLogEntry.FILE_DELETE) {
                    String fileName = logEntry.getFileName();
                    if (filesDirectlyWrittenToDisk.contains(new File(fileName))) {
                        continue;
                    }
                    commitDeleteFile(fileName);
                } else if (logEntry.getOperationType() == TransactionLogEntry.FILE_CREATE) {
                    String fileName = logEntry.getFileName();
                    if (filesDirectlyWrittenToDisk.contains(new File(fileName))) {
                        continue;
                    }
                    commitCreateFile(fileName);
                } else if (logEntry.getOperationType() == TransactionLogEntry.DIR_CREATE) {
                    commitCreateDir(logEntry.getFileName());
                } else if (logEntry.getOperationType() == TransactionLogEntry.FILE_COPY) {
                    File dest = new File(logEntry.getDestFileName());
                    if (filesDirectlyWrittenToDisk.contains(dest)) {
                        continue;
                    }
                    commitFileCopy(logEntry, i);
                } else if (logEntry.getOperationType() == TransactionLogEntry.FILE_MOVE) {
                    File dest = new File(logEntry.getDestFileName());
                    if (filesDirectlyWrittenToDisk.contains(dest)) {
                        continue;
                    }
                    commitFileMove(logEntry, i);
                } else if (logEntry.getOperationType() == TransactionLogEntry.FILE_TRUNCATE) {
                    String fileName = logEntry.getFileName();
                    if (filesDirectlyWrittenToDisk.contains(new File(fileName))) {
                        continue;
                    }
                    commitFileTruncate(logEntry);
                } else if (logEntry.getOperationType() == TransactionLogEntry.FILE_SPECIAL_MOVE) {
                    commitFileSpecialMove(logEntry, i);
                }
            }
            xaFileSystem.getTheGatheringDiskWriter().transactionCompletes(xid, true);
            for (FileChannel logChannel : logReaderChannels.values()) {
                logChannel.close();
            }
            cleanup();
            //we should not raise events before actually making the commit changes; else the MDBs
            //wouldn't find what they wanted to look for. The below is in-memory operation, so
            //after a crash, we need to check enQed, but not deQed, events for all committed txns
            //and populate them in in-memory queue again.
            raiseFileStateChangeEvents();
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailure(ioe);
        } finally {
            asynchronousRollbackLock.unlock();
        }
    }

    private void commitFileAppend(TransactionLogEntry logEntry, ByteBuffer inMemoryLogEntry,
            FileChannel logReaderChannel, int logFileIndex, long localPosition)
            throws IOException {
        String fileName = logEntry.getFileName();
        if (!new File(fileName).exists()) {
            return;
        }

        FileOutputStream fos = new FileOutputStream(fileName, true);
        long contentLength = logEntry.getFileContentLength();
        FileChannel fc = fos.getChannel();
        if (logFileIndex == -1) {
            long num = 0;
            inMemoryLogEntry.position(logEntry.getHeaderLength());
            while (num < contentLength) {
                num += fc.write(inMemoryLogEntry, logEntry.getFilePosition());
            }

        } else {
            logReaderChannel.position(localPosition + logEntry.getHeaderLength());
            long num = 0;
            if (logEntry.getFilePosition() <= fc.size()) {
                while (num < contentLength) {
                    num += fc.transferFrom(logReaderChannel, num + logEntry.getFilePosition(),
                            contentLength - num);
                }
            }
        }
        fc.force(false);
        fc.close();
    }

    private void commitDeleteFile(String fileName) throws IOException {
        File f = new File(fileName);
        if (f.exists()) {
            FileIOUtility.deleteFile(f);
        }
    }

    private void commitCreateFile(String fileName) throws IOException {
        File f = new File(fileName);
        if (f.exists()) {
            FileIOUtility.deleteFile(f);
        }
        FileIOUtility.createFile(f);
    }

    private void commitCreateDir(String fileName) throws IOException {
        File f = new File(fileName);
        if (f.exists()) {
            FileIOUtility.deleteDirectoryRecursively(f);
        }
        FileIOUtility.createDirectory(f);
    }

    private void commitFileCopy(TransactionLogEntry logEntry, int checkPointPosition) throws IOException {
        File src = new File(logEntry.getFileName());
        File dest = new File(logEntry.getDestFileName());
        if (dest.exists()) {
            FileIOUtility.deleteFile(dest);
        }

        FileChannel srcChannel = new FileInputStream(src).getChannel();
        FileChannel destChannel = new FileOutputStream(dest).getChannel();
        long contentLength = srcChannel.size();
        long num = 0;
        while (num < contentLength) {
            num += srcChannel.transferTo(num, contentLength - num, destChannel);
        }

        destChannel.force(false);
        srcChannel.close();
        destChannel.close();
        ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, checkPointPosition));
        xaFileSystem.getTheGatheringDiskWriter().forceLog(logEntryBytes);
    }

    private void commitFileMove(TransactionLogEntry logEntry, int checkPointPosition) throws IOException {
        File src = new File(logEntry.getFileName());
        File dest = new File(logEntry.getDestFileName());
        if (!src.exists()) {
            return;
        }

        if (dest.isDirectory()) {
            FileIOUtility.deleteDirectoryRecursively(dest);
        } else if (dest.exists()) {
            FileIOUtility.deleteFile(dest);
        }
        FileIOUtility.renameTo(src, dest);
        ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, checkPointPosition));
        xaFileSystem.getTheGatheringDiskWriter().forceLog(logEntryBytes);
    }

    private void commitFileTruncate(TransactionLogEntry logEntry) throws IOException {
        String fileName = logEntry.getFileName();
        if (!new File(fileName).exists()) {
            return;
        }

        FileOutputStream fos = new FileOutputStream(fileName, true);
        FileChannel fc = fos.getChannel();
        fc.truncate(logEntry.getNewLength());
        fc.force(false);
        fc.close();
    }

    private void commitFileSpecialMove(TransactionLogEntry logEntry, int checkPointPosition) throws IOException {
        File src = new File(logEntry.getFileName());
        File dest = new File(logEntry.getDestFileName());
        if (!src.exists()) {
            return;
        }
        if (dest.exists()) {
            FileIOUtility.deleteFile(dest);
        }
        FileIOUtility.renameTo(src, dest);
        ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid, checkPointPosition));
        xaFileSystem.getTheGatheringDiskWriter().forceLog(logEntryBytes);
    }

    private void raiseFileStateChangeEvents() {
        if (publishFileStateChangeEventsOnCommit) {
            xaFileSystem.getFileSystemEventQueue().addAll(fileStateChangeEventsToRaise);
        }
    }

    public void rollback() throws TransactionRolledbackException {
        try {
            asynchronousRollbackLock.lock();
            checkIfCanContinue();

            releaseAllStreams();

            ArrayList<Long> logPositions;
            HashMap<Integer, FileChannel> logReaderChannels = new HashMap<Integer, FileChannel>(2);
            FileChannel logReaderChannel = null;
            String transactionLogBaseName = xaFileSystem.getTransactionLogFileBaseName();
            if (createdForRecovery) {
                logPositions = xaFileSystem.getRecoveryWorker().getTransactionLogsPositions(xid);
            } else {
                HashSet<VirtualViewFile> filesTouchedInPlace = view.getViewFilesWithLatestViewOnDisk();
                for (VirtualViewFile vvf : filesTouchedInPlace) {
                    vvf.freePhysicalChannel();
                }
                logPositions = this.transactionLogPositions;
            }

            Buffer inMemoryLog;
            for (int i = logPositions.size() - 2; i >= 0; i -= 2) {
                ByteBuffer temp = null;
                int logFileIndex = logPositions.get(i).intValue();
                long localPosition = logPositions.get(i + 1);
                TransactionLogEntry logEntry;

                if (logFileIndex == -1) {
                    inMemoryLog = transactionInMemoryBuffers.get((int) localPosition);
                    temp =
                            inMemoryLog.getBuffer();
                    temp.position(0);
                    logEntry =
                            TransactionLogEntry.parseLogEntry(temp);
                } else {
                    if (!logReaderChannels.containsKey(logFileIndex)) {
                        logReaderChannels.put(logFileIndex, new FileInputStream(transactionLogBaseName + "_" + logFileIndex).getChannel());
                    }

                    logReaderChannel = logReaderChannels.get(logFileIndex);
                    logReaderChannel.position(localPosition);
                    logEntry =
                            TransactionLogEntry.getNextTransactionLogEntry(logReaderChannel, localPosition, false);
                }

                FileOutputStream fos;
                if (logEntry.getOperationType() == TransactionLogEntry.UNDOABLE_FILE_TRUNCATE) {
                    String fileName = logEntry.getFileName();
                    fos = new FileOutputStream(fileName, true);
                    long contentLength = logEntry.getFileContentLength();
                    FileChannel fc = fos.getChannel();
                    if (logFileIndex == -1) {
                    } else {
                        logReaderChannel.position(localPosition + logEntry.getHeaderLength());
                        long num = 0;
                        if (logEntry.getFilePosition() <= fc.size()) {
                            while (num < contentLength) {
                                num += fc.transferFrom(logReaderChannel, num + logEntry.getFilePosition(),
                                        contentLength - num);
                            }
                        }
                    }
                    fc.force(false);//improve this. force for every piece of content? (same in commit method).
                    fc.close();
                } else if (logEntry.getOperationType() == TransactionLogEntry.UNDOABLE_FILE_APPEND) {
                    String fileName = logEntry.getFileName();
                    fos = new FileOutputStream(fileName, true);
                    FileChannel fc = fos.getChannel();
                    fc.truncate(logEntry.getNewLength());
                    fc.force(false);//the file length may be part of meta-data (not sure). Make "true"?
                    fc.close();
                }

            }
            xaFileSystem.getTheGatheringDiskWriter().transactionCompletes(xid, false);
            for (FileChannel logChannel : logReaderChannels.values()) {
                logChannel.close();
            }

            cleanup();
        } catch (IOException ioe) {
            xaFileSystem.notifySystemFailure(ioe);
        } finally {
            asynchronousRollbackLock.unlock();
        }

    }

    private void cleanup() throws IOException {
        if (createdForRecovery) {
            xaFileSystem.getRecoveryWorker().cleanupTransactionInfo(xid);
        } else {
            xaFileSystem.getTheGatheringDiskWriter().cleanupTransactionInfo(xid);
        }

        releaseAllLocks();
        xaFileSystem.removeTransactionSessionEntry(xid);

        if (!createdForRecovery) {
            Iterator<VirtualViewFile> vvfsInBackupDir = view.getViewFilesUsingBackupDir().iterator();
            while (vvfsInBackupDir.hasNext()) {
                vvfsInBackupDir.next().cleanupBackup();
            }
            xaFileSystem.releaseRenamePinOnDirectories(directoriesPinnedInThisSession);
        }

        for (Buffer buffer : transactionInMemoryBuffers) {
            if (buffer instanceof PooledBuffer) {
                xaFileSystem.getBufferPool().checkIn((PooledBuffer) buffer);
            }
        }
    }

    private void releaseAllLocks() {
        for (Lock lock : allAcquiredLocks.values()) {
            xaFileSystem.releaseLock(xid, lock);
        }

        allAcquiredLocks.clear();
        RDG.removeNodeForTransaction(xid);
    }

    private void releaseAllStreams() throws TransactionRolledbackException {
        for (XAFileInputStream xafis : allAcquiredInputStreams) {
            xafis.close();
        }

        for (NativeXAFileOutputStream xafos : allAcquiredOutputStreams) {
            xafos.close();
            NativeXAFileOutputStream.deCacheXAFileOutputStream(xafos.getDestinationFile());
        }

    }

    public int getTransactionTimeout() {
        return transactionTimeout;
    }

    public boolean setTransactionTimeout(int transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
        return true;
    }

    private void checkPermission(int operation, File f) throws InsufficientPermissionOnFileException {
        switch (operation) {
            case READ_FILE:
                if (view.isNormalFileReadable(f)) {
                    return;
                }

                break;
            case WRITE_FILE:
                if (view.isNormalFileWritable(f)) {
                    return;
                }

                break;
        }

        throw new InsufficientPermissionOnFileException();
    }

    private Lock acquireLockIfRequired(File f, boolean exclusive) throws LockingFailedException,
            InterruptedException, TransactionRolledbackException {
        Lock newLock = null;
        if (!alreadyHaveALock(f, exclusive)) {
            if (exclusive) {
                newLock = xaFileSystem.acquireExclusiveLock(xid, f, fileLockWaitTimeout);
            } else {
                newLock = xaFileSystem.acquireSharedLock(xid, f, fileLockWaitTimeout);
            }

        }
        return newLock;
    }

    private boolean alreadyHaveALock(File f, boolean exclusive) {
        Lock existingLock = allAcquiredLocks.get(f);
        if (existingLock == null) {
            return false;
        }

        if (existingLock.isExclusive() || !exclusive) {
            return true;
        }

        return false;
    }

    private void checkValidParent(File f) throws FileNotExistsException {
        if (f.getParentFile() == null) {
            throw new FileNotExistsException(f.getParentFile());
        }
    }

    private void releaseLocks(Lock locks[]) {
        for (int i = 0; i < locks.length; i++) {
            if (locks[i] != null) {
                xaFileSystem.releaseLock(xid, locks[i]);
            }

        }
    }

    private void releaseLocks(Lock lock) {
        if (lock != null) {
            xaFileSystem.releaseLock(xid, lock);
        }

    }

    private void addLocks(Lock locks[]) {
        for (int i = 0; i < locks.length; i++) {
            if (locks[i] != null && !locks[i].isUpgraded()) {
                this.allAcquiredLocks.put(locks[i].getResource(), locks[i]);
            }

        }
    }

    private void addLocks(Lock lock) {
        if (lock != null && !lock.isUpgraded()) {
            this.allAcquiredLocks.put(lock.getResource(), lock);
        }

    }

    public long getFileLockWaitTimeout() {
        return fileLockWaitTimeout;
    }

    public void setFileLockWaitTimeout(long fileLockWaitTimeout) {
        this.fileLockWaitTimeout = fileLockWaitTimeout;
    }

    public void checkIfCanContinue() throws TransactionRolledbackException {
        if (rolledback) {
            throw new TransactionRolledbackException(rollbackCause);
        }

        if (systemHasFailed) {
            throw new XASystemException(systemFailureCause);
        }
    }

    public void declareTransactionUsingUndoLogs() throws IOException {
        ByteBuffer logEntryBytes = ByteBuffer.wrap(TransactionLogEntry.getLogEntry(xid,
                TransactionLogEntry.TXN_USES_UNDO_LOGS));
        xaFileSystem.getTheGatheringDiskWriter().forceLog(logEntryBytes);
    }

    public long getTimeOfEntryToTransaction() {
        return timeOfEntryToTransaction;
    }

    public ReentrantLock getAsynchronousRollbackLock() {
        return asynchronousRollbackLock;
    }

    public void addLogPositionToTransaction(int logFileIndex, long localPosition) {
        transactionLogPositions.add((long) logFileIndex);
        transactionLogPositions.add(localPosition);
    }

    public void addInMemoryBufferToTransaction(Buffer buffer) {
        transactionInMemoryBuffers.add(buffer);
        int indexIntoBufferArray = transactionInMemoryBuffers.size() - 1;
        addLogPositionToTransaction(-1, indexIntoBufferArray);
    }

    public int getNumOwnedExclusiveLocks() {
        return numOwnedExclusiveLocks;
    }

    void incrementNumOwnedExclusiveLocks() {
        this.numOwnedExclusiveLocks++;
    }

    boolean hasStartedCommitting() {
        return startedCommitting;
    }

    public XidImpl getXid() {
        return xid;
    }

    private void checkAnyOpenStreamToDescendantFiles(File ancestor) throws FileUnderUseException {
        for (NativeXAFileInputStream is : allAcquiredInputStreams) {
            if (NativeXAFileSystem.isAncestorOf(ancestor, is.getSourceFileName()) && !is.isClosed()) {
                throw new FileUnderUseException("First close the input stream to file: " + is.getSourceFileName());
            }
        }
        for (NativeXAFileOutputStream os : allAcquiredOutputStreams) {
            if (NativeXAFileSystem.isAncestorOf(ancestor, os.getDestinationFile()) && !os.isClosed()) {
                throw new FileUnderUseException("First close the input stream to file: " + os.getDestinationFile());
            }
        }
    }

    public boolean getPublishFileStateChangeEventsOnCommit() {
        return publishFileStateChangeEventsOnCommit;
    }

    public void setPublishFileStateChangeEventsOnCommit(boolean publishFileStateChangeEventsOnCommit) {
        this.publishFileStateChangeEventsOnCommit = publishFileStateChangeEventsOnCommit;
    }

    public void reAssociateTransactionThread(Thread thread) {
        //this check is required due to quite possible calls from Txn Manager even after committing
        //the txn on RemoteManagedConnection (such NPE was seen also).
        ResourceDependencyGraph.Node xidNode = this.xid.getNodeInResourceDependencyGraph();
        if (xidNode != null) {
            xidNode.reAssociatedTransactionThread(thread);
        }
    }

    private void addToFileSystemEvents(byte actionType, File affectedObject, boolean isDirectory) {
        File parentDirectory = null;
        switch (actionType) {
            case FileStateChangeEvent.FILE_CREATED:
                fileStateChangeEventsToRaise.add(new FileStateChangeEvent(affectedObject, isDirectory, FileStateChangeEvent.FILE_CREATED, xid));
                parentDirectory = affectedObject.getParentFile();
                if (parentDirectory != null) {
                    fileStateChangeEventsToRaise.add(new FileStateChangeEvent(parentDirectory, true, FileStateChangeEvent.FILE_MODIFIED, xid));
                }
                break;
            case FileStateChangeEvent.FILE_DELETED:
                fileStateChangeEventsToRaise.add(new FileStateChangeEvent(affectedObject, isDirectory, FileStateChangeEvent.FILE_DELETED, xid));
                parentDirectory = affectedObject.getParentFile();
                if (parentDirectory != null) {
                    fileStateChangeEventsToRaise.add(new FileStateChangeEvent(parentDirectory, true, FileStateChangeEvent.FILE_MODIFIED, xid));
                }
                break;
            case FileStateChangeEvent.FILE_MODIFIED:
                fileStateChangeEventsToRaise.add(new FileStateChangeEvent(affectedObject, isDirectory, FileStateChangeEvent.FILE_MODIFIED, xid));
                break;
        }
    }

    private void addToFileSystemEvents(byte[] actionType, File[] affectedObject, boolean areDirectories) {
        for (int i = 0; i < actionType.length; i++) {
            addToFileSystemEvents(actionType[i], affectedObject[i], areDirectories);
        }
    }

    public void commit() throws TransactionRolledbackException {
        this.commit(true);
    }
}

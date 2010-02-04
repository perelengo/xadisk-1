package org.xadisk.filesystem;

import org.xadisk.filesystem.exceptions.DeadLockVictimizedException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.filesystem.exceptions.AncestorPinnedException;
import org.xadisk.filesystem.exceptions.DirectoryPinningFailedException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.LockingTimedOutException;
import org.xadisk.filesystem.exceptions.RecoveryInProgressException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;
import org.xadisk.filesystem.exceptions.TransactionTimeoutException;
import org.xadisk.filesystem.exceptions.XASystemException;

public class XAFileSystem {

    private static XAFileSystem theXAFileSystem;
    private final AtomicLong lastTransactionId = new AtomicLong(System.currentTimeMillis() / 1000);
    private final ConcurrentHashMap<File, Lock> fileLocks = new ConcurrentHashMap<File, Lock>(1000);
    private final BufferPool bufferPool;
    private final String transactionLogFileBaseName;
    private static Logger logger;
    private final String XADiskHome;
    private final String transactionLogsDir;
    private static FileSystemConfiguration configuration;
    private final ConcurrentHashMap<XidImpl, Session> transactionAndSession = new ConcurrentHashMap<XidImpl, Session>(1000);
    private HashSet<XidImpl> transactionsPreparedPreCrash;
    private boolean returnedAllPreparedTransactions = false;
    private static WorkManager workManager;
    private final ResourceDependencyGraph resourceDependencyGraph;
    private final GatheringDiskWriter gatheringDiskWriter;
    private final CrashRecoveryWorker recoveryWorker;
    private final BufferPoolReliever bufferPoolReliever;
    private final DeadLockDetector deadLockDetector;
    private final FileSystemEventDelegator fileSystemEventDelegator;
    private final TransactionTimeoutDetector transactionTimeoutDetector;
    private final int lockTimeOut;
    private boolean recoveryComplete = false;
    private final LinkedBlockingQueue<FileStateChangeEvent> fileSystemEventQueue;
    private volatile boolean systemHasFailed = false;
    private volatile Throwable systemFailureCause = null;
    private final HashMap<File, XidImpl> directoriesPinnedForRename = new HashMap<File, XidImpl>(1000);
    private final CriticalWorkersListener workListener;
    private final File topLevelBackupDir;
    private File currentBackupDirPath;
    private AtomicLong backupFileNameCounter = new AtomicLong(0);
    private final int maxFilesInBackupDirectory = 100000;
    private final int defaultTransactionTimeout;

    private XAFileSystem() {
        try {
            XADiskHome = configuration.getXaDiskHome();
            topLevelBackupDir = new File(XADiskHome, "backupDir");
            FileIOUtility.createDirectoriesIfRequired(new File(XADiskHome));
            if (!topLevelBackupDir.isDirectory()) {
                FileIOUtility.createDirectory(topLevelBackupDir);
            }
            XAFileSystem.logger = new Logger(new File(configuration.getXaDiskHome(), "debug.log"),
                    (byte) configuration.getLogLevel());
            transactionLogsDir = configuration.getXaDiskHome() + File.separator + "txnlogs";
            new File(transactionLogsDir).mkdirs();
            transactionLogFileBaseName = transactionLogsDir + File.separator + "xadisk.log";
            bufferPool = new BufferPool(configuration.getDirectBufferPoolSize(), configuration.getNonDirectBufferPoolSize(),
                    configuration.getBufferSize(), configuration.getDirectBufferIdleTime(),
                    configuration.getNonDirectBufferIdleTime(), this);
            gatheringDiskWriter = new GatheringDiskWriter(configuration.getCumulativeBufferSizeForDiskWrite(),
                    configuration.getTransactionLogFileMaxSize(), configuration.getMaxNonPooledBufferSize(),
                    transactionLogFileBaseName, this);
            recoveryWorker = new CrashRecoveryWorker(this);
            bufferPoolReliever = new BufferPoolReliever(bufferPool, configuration.getBufferPoolRelieverInterval());
            resourceDependencyGraph = new ResourceDependencyGraph();
            deadLockDetector = new DeadLockDetector(configuration.getDeadLockDetectorInterval(), resourceDependencyGraph,
                    this);
            transactionTimeoutDetector = new TransactionTimeoutDetector(1, this);
            this.fileSystemEventQueue = new LinkedBlockingQueue<FileStateChangeEvent>();
            this.fileSystemEventDelegator = new FileSystemEventDelegator(this, configuration.getMaximumConcurrentEventDeliveries());
            this.lockTimeOut = configuration.getLockTimeOut();
            this.defaultTransactionTimeout = configuration.getTransactionTimeout();
            this.workListener = new CriticalWorkersListener(this);

            workManager.startWork(deadLockDetector, 0, null, workListener);
            workManager.startWork(bufferPoolReliever, 0, null, workListener);
            workManager.startWork(fileSystemEventDelegator, 0, null, workListener);
            workManager.startWork(transactionTimeoutDetector, 0, null, workListener);

            recoveryWorker.collectLogFileNamesToProcess();
            gatheringDiskWriter.initialize();
            workManager.startWork(gatheringDiskWriter, 0, null, workListener);
            workManager.startWork(recoveryWorker, 0, null, workListener);

        } catch (Exception e) {
            throw new XASystemException(e);
        }
    }

    public static XAFileSystem bootXAFileSystem(FileSystemConfiguration configuration,
            WorkManager workManager) {
        XAFileSystem.configuration = configuration;
        XAFileSystem.workManager = workManager;
        theXAFileSystem = new XAFileSystem();
        return theXAFileSystem;
    }

    public static XAFileSystem bootXAFileSystemStandAlone(StandaloneFileSystemConfiguration configuration) {
        XAFileSystem.configuration = configuration;
        XAFileSystem.workManager = new StandaloneWorkManager(
                configuration.getWorkManagerCorePoolSize(), configuration.getWorkManagerMaxPoolSize(),
                configuration.getWorkManagerKeepAliveTime());
        theXAFileSystem = new XAFileSystem();
        return theXAFileSystem;
    }

    public static XAFileSystem getXAFileSystem() {
        return theXAFileSystem;
    }

    void notifyRecoveryComplete() throws IOException {
        ArrayList<FileStateChangeEvent> eventsNotYetDequeued = recoveryWorker.getFileSystemEventsFromCommittedTransactions();
        fileSystemEventQueue.addAll(eventsNotYetDequeued);
        FileIOUtility.deleteDirectoryRecursively(topLevelBackupDir);
        FileIOUtility.createDirectory(topLevelBackupDir);
        backupFileNameCounter.set(0);
        currentBackupDirPath = new File(topLevelBackupDir.getAbsolutePath(), "deeper");
        FileIOUtility.createDirectory(currentBackupDirPath);

        recoveryComplete = true;
    }

    public Session createSessionForLocalTransaction() {
        checkIfCanContinue();
        Session session = new Session(XidImpl.getXidInstanceForLocalTransaction(getNextLocalTransactionId()), false);
        return session;
    }

    public Session createSessionForXATransaction(XidImpl xid) {
        checkIfCanContinue();
        Session session = new Session(xid, false);
        return session;
    }

    public Session getSessionForTransaction(XidImpl xid) {
        Session session;
        session = transactionAndSession.get(xid);
        if (session != null) {
            return session;
        }
        if (transactionsPreparedPreCrash.contains(xid)) {
            session = new Session(xid, true);
            if (session != null) {
                return session;
            }
        }
        return null;
    }

    void removeTransactionSessionEntry(XidImpl xid) {
        transactionAndSession.remove(xid);
    }

    void assignSessionToTransaction(XidImpl xid, Session session) {
        transactionAndSession.put(xid, session);
    }

    Session[] getAllSessions() {
        return transactionAndSession.values().toArray(new Session[0]);
    }

    Session createRecoverySession(XidImpl xid) {
        return new Session(xid, true);
    }

    //todo : confirm that recover on XAR/here will be called only by one thread and not in parallel by more
    //then one thread.
    public Xid[] recover(int flag) throws XAException {
        if (!recoveryWorker.isRecoveryDataCollectionDone()) {
            throw new XAException(XAException.XAER_RMFAIL);
        }
        if (flag == XAResource.TMSTARTRSCAN) {
            returnedAllPreparedTransactions = false;
        }
        if (flag == (XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN)) {
            returnedAllPreparedTransactions = false;
        }
        if (returnedAllPreparedTransactions) {
            return new Xid[0];
        }
        this.transactionsPreparedPreCrash = recoveryWorker.getPreparedInDoubtTransactions();
        Xid xids[];
        xids = transactionsPreparedPreCrash.toArray(new Xid[0]);
        returnedAllPreparedTransactions = true;
        return xids;
    }

    void interruptTransactionIfWaitingForResourceLock(XidImpl xid, byte cause) {
        synchronized (xid.interruptFlagLock) {
            xid.setInterruptCause(cause);
            ResourceDependencyGraph.Node node = xid.getNodeInResourceDependencyGraph();
            if (node != null && node.isWaitingForResource()) {
                node.getTransactionThread().interrupt();
            }
        }
    }

    Lock acquireSharedLock(XidImpl requestor, File f, long time) throws
            LockingFailedException, InterruptedException, TransactionRolledbackException {
        Lock lock;

        lock = fileLocks.get(f);
        if (lock == null) {
            synchronized (directoriesPinnedForRename) {
                lock = fileLocks.get(f);
                if (lock == null) {
                    checkIfAnyAncestorPinnedForRename(f, requestor);
                    lock = new Lock(false, f);
                    lock.addHolder(requestor);
                    fileLocks.put(f, lock);
                    return lock;
                }
            }
        }

        try {
            lock.startSynchBlock();
            if (!lock.isExclusive()) {
                synchronized (directoriesPinnedForRename) {
                    checkIfAnyAncestorPinnedForRename(f, requestor);
                    lock.addHolder(requestor);
                    return lock;
                }
            }
            long remainingTime = time;
            boolean indefiniteWait = (time == 0);
            resourceDependencyGraph.addDependency(requestor, lock);
            while (lock.isExclusive()) {
                try {
                    long now1 = System.currentTimeMillis();
                    lock.waitTillReadable(remainingTime);
                    if (!lock.isExclusive()) {
                        break;
                    }
                    long now2 = System.currentTimeMillis();
                    remainingTime = remainingTime - (now2 - now1);
                    if (!indefiniteWait && remainingTime <= 0) {
                        removeDependencyFromRDG(requestor);
                        throw new LockingTimedOutException();
                    }
                } catch (InterruptedException ie) {
                    removeDependencyFromRDG(requestor);
                    if (requestor.getInterruptCause() == XidImpl.INTERRUPTED_DUE_TO_DEADLOCK) {
                        DeadLockVictimizedException dlve = new DeadLockVictimizedException();
                        requestor.getOwningSession().rollbackPrematurely(dlve);
                        throw new TransactionRolledbackException(dlve);
                    } else if (requestor.getInterruptCause() == XidImpl.INTERRUPTED_DUE_TO_TIMEOUT) {
                        TransactionTimeoutException ttoe = new TransactionTimeoutException();
                        requestor.getOwningSession().rollbackPrematurely(ttoe);
                        throw new TransactionRolledbackException(ttoe);
                    }
                    throw ie;
                }
            }
            removeDependencyFromRDG(requestor);
            synchronized (directoriesPinnedForRename) {
                checkIfAnyAncestorPinnedForRename(f, requestor);
                lock.addHolder(requestor);
                return lock;
            }
        } finally {
            lock.endSynchBlock();
        }
    }

    void removeDependencyFromRDG(XidImpl requestor) {
        synchronized (requestor.interruptFlagLock) {
            resourceDependencyGraph.removeDependency(requestor);
            Thread.interrupted();
        }
    }

    private boolean canUpgradeLock(Lock lock, XidImpl requestor) {
        return lock.getNumHolders() == 1 && lock.isAHolder(requestor);
    }

    public static boolean isAncestorOf(File a, File b) {
        File parentB = b.getParentFile();
        while (parentB != null) {
            if (a.equals(parentB)) {
                return true;
            }
            parentB = parentB.getParentFile();
        }
        return false;
    }
    
    private void checkIfAnyAncestorPinnedForRename(File f, XidImpl exceptionalSelf) throws AncestorPinnedException {
        File parent = f.getParentFile();
        while (parent != null) {
            XidImpl pinHolder = directoriesPinnedForRename.get(parent);
            if (pinHolder != null && !pinHolder.equals(exceptionalSelf)) {
                throw new AncestorPinnedException();
            }
            parent = parent.getParentFile();
        }
    }

    void pinDirectoryForRename(File dir, XidImpl exceptionalSelf) throws
            DirectoryPinningFailedException {
        synchronized (directoriesPinnedForRename) {
            for (File file : fileLocks.keySet()) {
                if (isAncestorOf(dir, file)) {
                    Lock lock = fileLocks.get(file);
                    Iterator<XidImpl> holders = lock.getHolders().iterator();
                    while (holders.hasNext()) {
                        if (!holders.next().equals(exceptionalSelf)) {
                            throw new DirectoryPinningFailedException();
                        }
                    }
                }
            }
            directoriesPinnedForRename.put(dir, exceptionalSelf);
        }
    }

    void releaseRenamePinOnDirectories(Collection<File> dirs) {
        synchronized (directoriesPinnedForRename) {
            for (File dir : dirs) {
                directoriesPinnedForRename.remove(dir);
            }
        }
    }

    void releaseRenamePinOnDirectory(File dir) {
        synchronized (directoriesPinnedForRename) {
            directoriesPinnedForRename.remove(dir);
        }
    }

    Lock acquireExclusiveLock(XidImpl requestor, File f, long time)
            throws LockingFailedException, InterruptedException, TransactionRolledbackException {
        Lock lock;

        lock = fileLocks.get(f);
        if (lock == null) {
            synchronized (directoriesPinnedForRename) {
                lock = fileLocks.get(f);
                if (lock == null) {
                    checkIfAnyAncestorPinnedForRename(f, requestor);
                    lock = new Lock(true, f);
                    lock.addHolder(requestor);
                    fileLocks.put(f, lock);
                    requestor.getOwningSession().incrementNumOwnedExclusiveLocks();
                    return lock;
                }
            }
        }

        try {
            lock.startSynchBlock();
            if (canUpgradeLock(lock, requestor)) {
                synchronized (directoriesPinnedForRename) {
                    checkIfAnyAncestorPinnedForRename(f, requestor);
                    lock.setExclusive(true);
                    lock.markUpgraded();
                    requestor.getOwningSession().incrementNumOwnedExclusiveLocks();
                    return lock;
                }
            }

            long remainingTime = time;
            boolean indefiniteWait = (time == 0);
            resourceDependencyGraph.addDependency(requestor, lock);
            while (!(lock.getNumHolders() == 0 || canUpgradeLock(lock, requestor))) {
                try {
                    long now1 = System.currentTimeMillis();
                    lock.waitTillWritable(remainingTime);
                    if (lock.getNumHolders() == 0 || canUpgradeLock(lock, requestor)) {
                        break;
                    }
                    long now2 = System.currentTimeMillis();
                    remainingTime = remainingTime - (now2 - now1);
                    if (!indefiniteWait && remainingTime <= 0) {
                        removeDependencyFromRDG(requestor);
                        throw new LockingTimedOutException();
                    }
                } catch (InterruptedException ie) {
                    removeDependencyFromRDG(requestor);
                    if (requestor.getInterruptCause() == XidImpl.INTERRUPTED_DUE_TO_DEADLOCK) {
                        DeadLockVictimizedException dlve = new DeadLockVictimizedException();
                        requestor.getOwningSession().rollbackPrematurely(dlve);
                        throw new TransactionRolledbackException(dlve);
                    } else if (requestor.getInterruptCause() == XidImpl.INTERRUPTED_DUE_TO_TIMEOUT) {
                        TransactionTimeoutException ttoe = new TransactionTimeoutException();
                        requestor.getOwningSession().rollbackPrematurely(ttoe);
                        throw new TransactionRolledbackException(ttoe);
                    }
                    throw ie;
                }
            }
            removeDependencyFromRDG(requestor);
            synchronized (directoriesPinnedForRename) {
                checkIfAnyAncestorPinnedForRename(f, requestor);
                lock.setExclusive(true);
                if (canUpgradeLock(lock, requestor)) {
                    lock.markUpgraded();
                } else {
                    lock.addHolder(requestor);
                }
                requestor.getOwningSession().incrementNumOwnedExclusiveLocks();
                return lock;
            }
        } finally {
            lock.endSynchBlock();
        }
    }

    void releaseLock(XidImpl releasor, Lock lock) {
        try {
            lock.startSynchBlock();
            //TODO: write a good code to delete unnecessary entries from the fileLocks map. Be careful :
            //for example, the earlier code here was bad and theoreticlly itself causes a lot of problems.
            lock.removeHolder(releasor);
            if (lock.isExclusive()) {
                lock.reset();
                lock.notifyReadWritable();
            } else {
                lock.notifyWritable();
            }
        } finally {
            lock.endSynchBlock();
        }
    }

    BufferPool getBufferPool() {
        return bufferPool;
    }

    public GatheringDiskWriter getTheGatheringDiskWriter() {
        return gatheringDiskWriter;
    }

    String getTransactionLogFileBaseName() {
        return transactionLogFileBaseName;
    }

    String getTransactionLogsDir() {
        return transactionLogsDir;
    }

    public CrashRecoveryWorker getRecoveryWorker() {
        return recoveryWorker;
    }

    int getConfiguredBufferSize() {
        return configuration.getBufferSize();
    }

    static WorkManager getWorkManager() {
        return workManager;
    }

    ResourceDependencyGraph getResourceDependencyGraph() {
        return resourceDependencyGraph;
    }

    public long getNextLocalTransactionId() {
        return lastTransactionId.getAndIncrement();
    }

    static Logger getLogger() {
        return logger;
    }

    public void shutdown() throws IOException {
        bufferPoolReliever.release();
        deadLockDetector.release();
        recoveryWorker.release();
        gatheringDiskWriter.release();
        gatheringDiskWriter.deInitialize();
        fileSystemEventDelegator.release();
        transactionTimeoutDetector.release();
        logger.releaseLogFile();
        getLogger().close();
        if (workManager instanceof StandaloneWorkManager) {
            ((StandaloneWorkManager) workManager).shutdown();
        }
    }

    int getLockTimeOut() {
        return lockTimeOut;
    }

    File getNextBackupFileName() throws IOException {
        File savedCurrentBackupDir = this.currentBackupDirPath;
        long nextBackupFileName = backupFileNameCounter.getAndIncrement();
        if (nextBackupFileName >= maxFilesInBackupDirectory) {
            if (nextBackupFileName == maxFilesInBackupDirectory) {
                currentBackupDirPath = new File(currentBackupDirPath, "deeper");
                FileIOUtility.createDirectory(currentBackupDirPath);
                backupFileNameCounter.set(0);
            } else {
                while (backupFileNameCounter.get() >= maxFilesInBackupDirectory) {
                }
            }
        }
        return new File(savedCurrentBackupDir, nextBackupFileName + "");
    }

    LinkedBlockingQueue<FileStateChangeEvent> getFileSystemEventQueue() {
        return fileSystemEventQueue;
    }

    public FileSystemEventDelegator getFileSystemEventDelegator() {
        return fileSystemEventDelegator;
    }

    public void notifySystemFailure(Throwable systemFailureCause) {
        this.systemHasFailed = true;
        this.systemFailureCause = systemFailureCause;
        Session allSessions[];
        allSessions = transactionAndSession.values().toArray(new Session[0]);
        for (int i = 0; i < allSessions.length; i++) {
            allSessions[i].notifySystemFailure(systemFailureCause);
        }
        throw new XASystemException(systemFailureCause);
    }

    public void notifySystemFailureAndContinue(Throwable systemFailureCause) {
        try {
            notifySystemFailure(systemFailureCause);
        } catch (XASystemException xase) {
        }
    }

    public void checkIfCanContinue() {
        if (!recoveryComplete) {
            if (systemFailureCause != null) {
                throw new RecoveryInProgressException(systemFailureCause);
            } else {
                throw new RecoveryInProgressException(null);
            }
        }
        if (systemHasFailed) {
            throw new XASystemException(systemFailureCause);
        }
    }

    public int getDefaultTransactionTimeout() {
        return defaultTransactionTimeout;
    }
}

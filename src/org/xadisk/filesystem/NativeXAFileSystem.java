/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.filesystem;

import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.bridge.proxies.interfaces.XASession;
import org.xadisk.filesystem.pools.BufferPool;
import org.xadisk.filesystem.utilities.Logger;
import org.xadisk.filesystem.workers.observers.CriticalWorkersListener;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;
import org.xadisk.filesystem.standalone.StandaloneWorkManager;
import org.xadisk.filesystem.workers.CrashRecoveryWorker;
import org.xadisk.filesystem.workers.DeadLockDetector;
import org.xadisk.filesystem.workers.FileSystemEventDelegator;
import org.xadisk.filesystem.workers.GatheringDiskWriter;
import org.xadisk.filesystem.workers.ObjectPoolReliever;
import org.xadisk.filesystem.workers.TransactionTimeoutDetector;
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
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.connector.inbound.EndPointActivation;
import org.xadisk.filesystem.exceptions.AncestorPinnedException;
import org.xadisk.filesystem.exceptions.DirectoryPinningFailedException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.LockingTimedOutException;
import org.xadisk.filesystem.exceptions.RecoveryInProgressException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;
import org.xadisk.filesystem.exceptions.TransactionTimeoutException;
import org.xadisk.filesystem.exceptions.XASystemException;
import org.xadisk.bridge.proxies.facilitators.RemoteMethodInvoker;
import org.xadisk.bridge.proxies.impl.RemoteMessageEndpointFactory;
import org.xadisk.bridge.proxies.impl.RemoteXAFileSystem;
import org.xadisk.bridge.server.conversation.GlobalHostedContext;
import org.xadisk.bridge.server.PointOfContact;
import org.xadisk.connector.inbound.DeadLetterMessageEndpoint;
import org.xadisk.connector.inbound.LocalEventProcessingXAResource;
import org.xadisk.filesystem.exceptions.XASystemBootFailureException;
import org.xadisk.filesystem.exceptions.XASystemNoMoreAvailableException;
import org.xadisk.filesystem.pools.SelectorPool;

public class NativeXAFileSystem implements XAFileSystemCommonness {

    private static ConcurrentHashMap<String, NativeXAFileSystem> allXAFileSystems = new ConcurrentHashMap<String, NativeXAFileSystem>();
    private final AtomicLong lastTransactionId = new AtomicLong(System.currentTimeMillis() / 1000);
    private final ConcurrentHashMap<File, Lock> fileLocks = new ConcurrentHashMap<File, Lock>(1000);
    private final BufferPool bufferPool;
    private final SelectorPool selectorPool;
    private final String transactionLogFileBaseName;
    private Logger logger;
    private final String XADiskHome;
    private final String transactionLogsDir;
    private final DeadLetterMessageEndpoint deadLetter;
    private final FileSystemConfiguration configuration;
    private final ConcurrentHashMap<XidImpl, NativeSession> transactionAndSession = new ConcurrentHashMap<XidImpl, NativeSession>(1000);
    private HashSet<XidImpl> transactionsPreparedPreCrash;
    private boolean returnedAllPreparedTransactions = false;
    private final WorkManager workManager;
    private final ResourceDependencyGraph resourceDependencyGraph;
    private final GatheringDiskWriter gatheringDiskWriter;
    private final CrashRecoveryWorker recoveryWorker;
    private final ObjectPoolReliever bufferPoolReliever;
    private final ObjectPoolReliever selectorPoolReliever;
    private final DeadLockDetector deadLockDetector;
    private final FileSystemEventDelegator fileSystemEventDelegator;
    private final TransactionTimeoutDetector transactionTimeoutDetector;
    private final PointOfContact pointOfContact;
    private final int lockTimeOut;
    private boolean recoveryComplete = false;
    private final LinkedBlockingQueue<FileSystemStateChangeEvent> fileSystemEventQueue;
    private volatile boolean systemHasFailed = false;
    private volatile Throwable systemFailureCause = null;
    private final HashMap<File, XidImpl> directoriesPinnedForRename = new HashMap<File, XidImpl>(1000);
    private final CriticalWorkersListener workListener;
    private final File topLevelBackupDir;
    private File currentBackupDirPath;
    private AtomicLong backupFileNameCounter = new AtomicLong(0);
    private final int maxFilesInBackupDirectory = 100000;
    private final int defaultTransactionTimeout;
    private final GlobalHostedContext globalCallbackContext = new GlobalHostedContext();
    private final AtomicLong totalNonPooledBufferSize = new AtomicLong(0);

    private NativeXAFileSystem(FileSystemConfiguration configuration,
            WorkManager workManager) {
        this.configuration = configuration;
        this.workManager = workManager;
        try {
            XADiskHome = configuration.getXaDiskHome();
            topLevelBackupDir = new File(XADiskHome, "backupDir");
            
            DurableDiskSession.setSynchronizeDirectoryChanges(configuration.getSynchronizeDirectoryChanges());

            DurableDiskSession diskSession = new DurableDiskSession();
            diskSession.createDirectoriesIfRequired(new File(XADiskHome));
            if (!topLevelBackupDir.isDirectory()) {
                diskSession.createDirectory(topLevelBackupDir);
            }
            this.logger = new Logger(new File(XADiskHome, "debug.log"), (byte) 3);
            transactionLogsDir = XADiskHome + File.separator + "txnlogs";
            diskSession.createDirectoriesIfRequired(new File(transactionLogsDir));
            transactionLogFileBaseName = transactionLogsDir + File.separator + "xadisk.log";
            bufferPool = new BufferPool(configuration.getDirectBufferPoolSize(), configuration.getNonDirectBufferPoolSize(),
                    configuration.getBufferSize(), configuration.getDirectBufferIdleTime(),
                    configuration.getNonDirectBufferIdleTime(), this);
            selectorPool = new SelectorPool(1000);
            gatheringDiskWriter = new GatheringDiskWriter(configuration.getCumulativeBufferSizeForDiskWrite(),
                    configuration.getTransactionLogFileMaxSize(), configuration.getMaxNonPooledBufferSize(),
                    transactionLogFileBaseName, this);
            recoveryWorker = new CrashRecoveryWorker(this);
            bufferPoolReliever = new ObjectPoolReliever(bufferPool, configuration.getBufferPoolRelieverInterval(), this);
            selectorPoolReliever = new ObjectPoolReliever(selectorPool, 1000, this);
            resourceDependencyGraph = new ResourceDependencyGraph();
            deadLockDetector = new DeadLockDetector(configuration.getDeadLockDetectorInterval(), resourceDependencyGraph,
                    this);
            transactionTimeoutDetector = new TransactionTimeoutDetector(1, this);
            this.fileSystemEventQueue = new LinkedBlockingQueue<FileSystemStateChangeEvent>();
            this.fileSystemEventDelegator = new FileSystemEventDelegator(this, configuration.getMaximumConcurrentEventDeliveries());
            this.lockTimeOut = configuration.getLockTimeOut();
            this.defaultTransactionTimeout = configuration.getTransactionTimeout();
            this.workListener = new CriticalWorkersListener(this);
            File deadLetterDir = new File(XADiskHome, "deadletter");
            diskSession.createDirectoriesIfRequired(deadLetterDir);
            this.deadLetter = new DeadLetterMessageEndpoint(deadLetterDir, this);

            diskSession.forceToDisk();
            
            workManager.startWork(deadLockDetector, WorkManager.INDEFINITE, null, workListener);
            workManager.startWork(bufferPoolReliever, WorkManager.INDEFINITE, null, workListener);
            workManager.startWork(selectorPoolReliever, WorkManager.INDEFINITE, null, workListener);
            workManager.startWork(fileSystemEventDelegator, WorkManager.INDEFINITE, null, workListener);
            workManager.startWork(transactionTimeoutDetector, WorkManager.INDEFINITE, null, workListener);
            if(configuration.getEnableRemoteInvocations()) {
                pointOfContact = new PointOfContact(this, configuration.getServerPort());
                workManager.startWork(pointOfContact, WorkManager.INDEFINITE, null, workListener);
            } else {
                pointOfContact = null;
            }

            recoveryWorker.collectRecoveryData();
            gatheringDiskWriter.initialize();
            workManager.startWork(gatheringDiskWriter, WorkManager.INDEFINITE, null, workListener);
            workManager.startWork(recoveryWorker, WorkManager.INDEFINITE, null, workListener);

        } catch (Exception e) {
            throw new XASystemBootFailureException(e);
        }
    }

    public static NativeXAFileSystem bootXAFileSystem(FileSystemConfiguration configuration,
            WorkManager workManager) {
        String instanceId = configuration.getInstanceId();
        if(allXAFileSystems.get(instanceId) != null) {
            throw new XASystemBootFailureException("An instance of XADisk with instance-id [" + instanceId + "] is already"
                    + " running in this JVM.");
        }
        NativeXAFileSystem newXAFileSystem = new NativeXAFileSystem(configuration, workManager);
        allXAFileSystems.put(configuration.getInstanceId(), newXAFileSystem);
        return newXAFileSystem;
    }

    public static NativeXAFileSystem bootXAFileSystemStandAlone(StandaloneFileSystemConfiguration configuration) {
        String instanceId = configuration.getInstanceId();
        if(allXAFileSystems.get(instanceId) != null) {
            throw new XASystemBootFailureException("An instance of XADisk with instance-id [" + instanceId + "] is already"
                    + " running in this JVM.");
        }
        WorkManager workManager = new StandaloneWorkManager(
                configuration.getWorkManagerCorePoolSize(), configuration.getWorkManagerMaxPoolSize(),
                configuration.getWorkManagerKeepAliveTime());
        NativeXAFileSystem newXAFileSystem = new NativeXAFileSystem(configuration, workManager);
        allXAFileSystems.put(configuration.getInstanceId(), newXAFileSystem);
        return newXAFileSystem;
    }

    public static NativeXAFileSystem getXAFileSystem(String instanceId) {
        return allXAFileSystems.get(instanceId);
    }

    public boolean pointToSameXAFileSystem(XAFileSystem xaFileSystem) {
        if(xaFileSystem instanceof NativeXAFileSystem && !(xaFileSystem instanceof RemoteXAFileSystem)) {
            NativeXAFileSystem that = (NativeXAFileSystem) xaFileSystem;
            return this.configuration.getInstanceId().equals(that.configuration.getInstanceId());
        } else {
            return false;
        }
    }

    public void notifyRecoveryComplete() throws IOException {
        fileSystemEventQueue.addAll(recoveryWorker.getEventsEnqueueCommittedNotDequeued());
        DurableDiskSession diskSession = new DurableDiskSession();
        diskSession.deleteDirectoryRecursively(topLevelBackupDir);
        diskSession.createDirectory(topLevelBackupDir);
        backupFileNameCounter.set(0);
        currentBackupDirPath = new File(topLevelBackupDir.getAbsolutePath(), "deeper");
        diskSession.createDirectory(currentBackupDirPath);
        diskSession.forceToDisk();
        recoveryComplete = true;
    }

    public NativeSession createSessionForLocalTransaction() {
        checkIfCanContinue();
        NativeSession session = new NativeSession(XidImpl.getXidInstanceForLocalTransaction(getNextLocalTransactionId()), false, this);
        return session;
    }

    public NativeSession createSessionForXATransaction(Xid xid) {
        checkIfCanContinue();
        NativeSession session = new NativeSession((XidImpl) xid, false, this);
        return session;
    }

    public XASession createSessionForXATransaction() {
        return new NativeXASession(this, configuration.getInstanceId());
    }

    public NativeSession getSessionForTransaction(Xid xid) {
        NativeSession session;
        session = transactionAndSession.get((XidImpl) xid);
        if (session != null) {
            return session;
        }
        if (transactionsPreparedPreCrash.contains((XidImpl) xid)) {
            ArrayList<FileSystemStateChangeEvent> events =
                    recoveryWorker.getEventsFromPreparedTransaction((XidImpl) xid);
            if (events != null) {
                session = new NativeSession((XidImpl) xid, events, this);
            } else {
                session = new NativeSession((XidImpl) xid, true, this);
            }
            if (session != null) {
                return session;
            }
        }
        return null;
    }

    void removeTransactionSessionEntry(XidImpl xid) {
        transactionAndSession.remove(xid);
    }

    void assignSessionToTransaction(XidImpl xid, NativeSession session) {
        transactionAndSession.put(xid, session);
    }

    public NativeSession[] getAllSessions() {
        Collection<NativeSession> sessions = transactionAndSession.values();
        return sessions.toArray(new NativeSession[sessions.size()]);
    }

    public NativeSession createRecoverySession(XidImpl xid, ArrayList<FileSystemStateChangeEvent> events) {
        if (events == null) {
            return new NativeSession(xid, true, this);
        } else {
            return new NativeSession(xid, events, this);
        }
    }

    //todo : confirm that recover on XAR/here will be called only by one thread and not in parallel by more
    //then one thread.
    public Xid[] recover(int flag) throws XAException {
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
        xids = transactionsPreparedPreCrash.toArray(new Xid[transactionsPreparedPreCrash.size()]);
        returnedAllPreparedTransactions = true;
        return xids;
    }

    public void interruptTransactionIfWaitingForResourceLock(XidImpl xid, byte cause) {
        synchronized (xid.interruptFlagLock) {
            xid.setInterruptCause(cause);
            ResourceDependencyGraph.Node node = xid.getNodeInResourceDependencyGraph();
            if (node != null && node.isWaitingForResource()) {
                node.getTransactionThread().interrupt();
            }
        }
    }

    Lock acquireFileLock(XidImpl requestor, File f, long time, boolean exclusive) throws
            LockingFailedException, InterruptedException, TransactionRolledbackException {
        if(exclusive) {
            return acquireExclusiveLock(requestor, f, time);
        } else {
            return acquireSharedLock(requestor, f, time);
        }
    }

    private Lock acquireSharedLock(XidImpl requestor, File f, long time) throws
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
                        throw new LockingTimedOutException(f.getAbsolutePath());
                    }
                } catch (InterruptedException ie) {
                    removeDependencyFromRDG(requestor);
                    if (requestor.getInterruptCause() == XidImpl.INTERRUPTED_DUE_TO_DEADLOCK) {
                        DeadLockVictimizedException dlve = new DeadLockVictimizedException(f.getAbsolutePath());
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
                throw new AncestorPinnedException(f.getAbsolutePath(), parent.getAbsolutePath());
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
                            throw new DirectoryPinningFailedException(dir.getAbsolutePath(), file.getAbsolutePath());
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

    private Lock acquireExclusiveLock(XidImpl requestor, File f, long time)
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
                        throw new LockingTimedOutException(f.getAbsolutePath());
                    }
                } catch (InterruptedException ie) {
                    removeDependencyFromRDG(requestor);
                    if (requestor.getInterruptCause() == XidImpl.INTERRUPTED_DUE_TO_DEADLOCK) {
                        DeadLockVictimizedException dlve = new DeadLockVictimizedException(f.getAbsolutePath());
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
            //TODO: write a good code to delete unnecessary entries from the fileLocks map.
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

    public BufferPool getBufferPool() {
        return bufferPool;
    }

    public SelectorPool getSelectorPool() {
        return selectorPool;
    }

    public GlobalHostedContext getGlobalCallbackContext() {
        return globalCallbackContext;
    }

    public GatheringDiskWriter getTheGatheringDiskWriter() {
        return gatheringDiskWriter;
    }

    public String getTransactionLogFileBaseName() {
        return transactionLogFileBaseName;
    }

    public String getTransactionLogsDir() {
        return transactionLogsDir;
    }

    public CrashRecoveryWorker getRecoveryWorker() {
        return recoveryWorker;
    }

    public int getConfiguredBufferSize() {
        return configuration.getBufferSize();
    }

    public WorkManager getWorkManager() {
        return workManager;
    }

    public ArrayList<EndPointActivation> getAllActivations() {
        return fileSystemEventDelegator.getAllActivations();
    }

    public void startWork(Work work) throws WorkException {
        workManager.startWork(work, WorkManager.INDEFINITE, null, workListener);
    }

    ResourceDependencyGraph getResourceDependencyGraph() {
        return resourceDependencyGraph;
    }

    public long getNextLocalTransactionId() {
        return lastTransactionId.getAndIncrement();
    }

    Logger getLogger() {
        return logger;
    }

    public void shutdown() throws IOException {
        
        NativeSession allSessions[];
        Collection<NativeSession> sessionsCollection = transactionAndSession.values();
        allSessions = sessionsCollection.toArray(new NativeSession[sessionsCollection.size()]);
        for (int i = 0; i < allSessions.length; i++) {
            allSessions[i].notifySystemShutdown();
        }
        
        bufferPoolReliever.release();
        selectorPoolReliever.release();
        deadLockDetector.release();
        recoveryWorker.release();
        gatheringDiskWriter.release();
        gatheringDiskWriter.deInitialize();
        fileSystemEventDelegator.release();
        transactionTimeoutDetector.release();
        if(configuration.getEnableRemoteInvocations()) {
            pointOfContact.release();
        }
        logger.releaseLogFile();
        getLogger().close();
        deadLetter.release();
        if (workManager instanceof StandaloneWorkManager) {
            ((StandaloneWorkManager) workManager).shutdown();
        }
        allXAFileSystems.remove(this.configuration.getInstanceId());
        DurableDiskSession.cleanUp();
    }

    int getLockTimeOut() {
        return lockTimeOut;
    }

    public File getNextBackupFileName() throws IOException {
        File savedCurrentBackupDir = this.currentBackupDirPath;
        long nextBackupFileName = backupFileNameCounter.getAndIncrement();
        if (nextBackupFileName >= maxFilesInBackupDirectory) {
            if (nextBackupFileName == maxFilesInBackupDirectory) {
                currentBackupDirPath = new File(currentBackupDirPath, "deeper");
                DurableDiskSession.createDirectoryDurably(currentBackupDirPath);
                backupFileNameCounter.set(0);
            } else {
                while (backupFileNameCounter.get() >= maxFilesInBackupDirectory) {
                }
            }
        }
        return new File(savedCurrentBackupDir, nextBackupFileName + "");
    }

    public LinkedBlockingQueue<FileSystemStateChangeEvent> getFileSystemEventQueue() {
        return fileSystemEventQueue;
    }

    public void registerEndPointActivation(EndPointActivation activation) throws IOException {
        boolean notADuplicateActivation = fileSystemEventDelegator.registerActivation(activation);
        if (notADuplicateActivation && activation.getMessageEndpointFactory() instanceof RemoteMessageEndpointFactory) {
            gatheringDiskWriter.recordEndPointActivation(activation);
            ((RemoteMessageEndpointFactory)activation.getMessageEndpointFactory()).setLocalXAFileSystem(this);
        }
    }

    public void deRegisterEndPointActivation(EndPointActivation activation) throws IOException {
        fileSystemEventDelegator.deRegisterActivation(activation);
        if (activation.getMessageEndpointFactory() instanceof RemoteMessageEndpointFactory) {
            gatheringDiskWriter.recordEndPointDeActivation(activation);
        }
    }

    FileSystemEventDelegator getFileSystemEventDelegator() {
        return fileSystemEventDelegator;
    }

    public void notifySystemFailure(Throwable systemFailureCause) {
        this.systemHasFailed = true;
        this.systemFailureCause = systemFailureCause;
        NativeSession allSessions[];
        Collection<NativeSession> sessionsCollection = transactionAndSession.values();
        allSessions = sessionsCollection.toArray(new NativeSession[sessionsCollection.size()]);
        for (int i = 0; i < allSessions.length; i++) {
            allSessions[i].notifySystemFailure(systemFailureCause);
        }
        throw new XASystemNoMoreAvailableException(systemFailureCause);
    }

    public void notifySystemFailureAndContinue(Throwable systemFailureCause) {
        try {
            notifySystemFailure(systemFailureCause);
        } catch (XASystemException xase) {
        }
    }

    public void checkIfCanContinue() {
        if (systemHasFailed) {
            throw new XASystemNoMoreAvailableException(systemFailureCause);
        }
        if (!recoveryComplete) {
            throw new RecoveryInProgressException();
        }
    }

    public void waitForBootup(long timeout) throws InterruptedException {
        if (timeout < 0) {
            while (true) {
                try {
                    checkIfCanContinue();
                    break;
                } catch (RecoveryInProgressException ripe) {
                    Thread.sleep(1000);
                }
            }
        } else {
            long timer = timeout;
            while (timer > 0) {
                try {
                    checkIfCanContinue();
                    break;
                } catch (RecoveryInProgressException ripe) {
                    Thread.sleep(1000);
                    timer -= 1000;
                }
            }
            checkIfCanContinue(); //to let the exception propagate to caller.
        }
    }

    public int getDefaultTransactionTimeout() {
        return defaultTransactionTimeout;
    }

    public String getXADiskSystemId() {
        return configuration.getServerAddress() + "_" + configuration.getServerPort();
    }

    public RemoteMethodInvoker createRemoteMethodInvokerToSelf() {
        return new RemoteMethodInvoker(configuration.getServerAddress(), configuration.getServerPort());
    }

    public XAResource getEventProcessingXAResourceForRecovery() {
        return new LocalEventProcessingXAResource(this);
    }

    public DeadLetterMessageEndpoint getDeadLetter() {
        return deadLetter;
    }

    public void changeTotalNonPooledBufferSize(int changeAmount) {
        totalNonPooledBufferSize.addAndGet(changeAmount);
    }

    public long getTotalNonPooledBufferSize() {
        return totalNonPooledBufferSize.get();
    }
}

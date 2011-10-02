package org.xadisk.filesystem;

import java.io.File;
import java.util.ArrayList;
import org.xadisk.filesystem.TransactionInformation;
import org.xadisk.filesystem.exceptions.AncestorPinnedException;
import org.xadisk.filesystem.exceptions.DeadLockVictimizedException;
import org.xadisk.filesystem.exceptions.DirectoryPinningFailedException;
import org.xadisk.filesystem.exceptions.LockingFailedException;
import org.xadisk.filesystem.exceptions.TransactionRolledbackException;
import org.xadisk.filesystem.exceptions.TransactionTimeoutException;

public interface ConcurrencyControl {

    public Lock acquireFileLock(TransactionInformation requestor, File f, long time, boolean exclusive)
            throws LockingFailedException, InterruptedException, TransactionRolledbackException,
            DeadLockVictimizedException, TransactionTimeoutException;

    public void releaseLock(TransactionInformation releasor, Lock lock);

    public void releaseRenamePinOnDirectories(ArrayList<File> dirs);

    public void releaseRenamePinOnDirectory(File dir);

    public void pinDirectoryForRename(File dir, TransactionInformation requestor)
            throws DirectoryPinningFailedException, AncestorPinnedException;

    public void interruptTransactionIfWaitingForResourceLock(TransactionInformation xid, byte cause);

    public void shutdown();
}

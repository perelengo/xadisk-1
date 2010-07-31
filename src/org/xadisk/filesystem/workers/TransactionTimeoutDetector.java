package org.xadisk.filesystem.workers;

import org.xadisk.filesystem.NativeSession;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.XidImpl;
import org.xadisk.filesystem.exceptions.TransactionTimeoutException;

public class TransactionTimeoutDetector extends TimedWorker {

    private final NativeXAFileSystem xaFileSystem;

    public TransactionTimeoutDetector(int frequency, NativeXAFileSystem xaFileSystem) {
        super(frequency);
        this.xaFileSystem = xaFileSystem;
    }

    @Override
    void doWorkOnce() {
        try {
            NativeSession sessions[] = xaFileSystem.getAllSessions();
            for (int i = 0; i < sessions.length; i++) {
                NativeSession session = sessions[i];
                int timeoutValue = session.getTransactionTimeout();
                int birthTime = (int) (session.getTimeOfEntryToTransaction() / 1000);
                int timeNow = (int) (System.currentTimeMillis() / 1000);
                if (timeoutValue > 0 && timeNow - birthTime > timeoutValue) {
                    xaFileSystem.interruptTransactionIfWaitingForResourceLock(session.getXid(), XidImpl.INTERRUPTED_DUE_TO_TIMEOUT);
                    session.rollbackAsynchronously(new TransactionTimeoutException());
                }
            }
        } catch (Throwable t) {
            xaFileSystem.notifySystemFailure(t);
        }
    }
}

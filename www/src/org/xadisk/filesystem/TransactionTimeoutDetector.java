package org.xadisk.filesystem;

import org.xadisk.filesystem.exceptions.TransactionTimeoutException;

class TransactionTimeoutDetector extends TimedWorker {

    private final XAFileSystem xaFileSystem;

    TransactionTimeoutDetector(int frequency, XAFileSystem xaFileSystem) {
        super(frequency);
        this.xaFileSystem = xaFileSystem;
    }

    @Override
    void doWorkOnce() {
        try {
            Session sessions[] = xaFileSystem.getAllSessions();
            for (int i = 0; i < sessions.length; i++) {
                Session session = sessions[i];
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

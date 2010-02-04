package org.xadisk.filesystem;

import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;

class CriticalWorkersListener implements WorkListener {

    private final XAFileSystem xaFileSystem;

    CriticalWorkersListener(XAFileSystem xaFileSystem) {
        this.xaFileSystem = xaFileSystem;
    }

    @Override
    public void workAccepted(WorkEvent we) {
    }

    @Override
    public void workCompleted(WorkEvent we) {
        if (we.getType() == WorkEvent.WORK_COMPLETED) {
            WorkException workException = we.getException();
            if (workException != null) {
                xaFileSystem.notifySystemFailure(workException);
            }
        }
    }

    @Override
    public void workRejected(WorkEvent we) {
    }

    @Override
    public void workStarted(WorkEvent we) {
    }
}

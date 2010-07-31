package org.xadisk.filesystem.workers.observers;

import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import org.xadisk.filesystem.NativeXAFileSystem;

public class CriticalWorkersListener implements WorkListener {

    private final NativeXAFileSystem xaFileSystem;

    public CriticalWorkersListener(NativeXAFileSystem xaFileSystem) {
        this.xaFileSystem = xaFileSystem;
    }

    public void workAccepted(WorkEvent we) {
    }

    public void workCompleted(WorkEvent we) {
        if (we.getType() == WorkEvent.WORK_COMPLETED) {
            //System.out.println("A Work completed..." + we.getSource());
            WorkException workException = we.getException();
            if (workException != null) {
                xaFileSystem.notifySystemFailure(workException);
            }
        }
    }

    public void workRejected(WorkEvent we) {
    }

    public void workStarted(WorkEvent we) {
    }
}

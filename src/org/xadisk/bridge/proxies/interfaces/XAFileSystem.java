package org.xadisk.bridge.proxies.interfaces;

import java.io.IOException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;
import org.xadisk.connector.inbound.EndPointActivation;
import org.xadisk.filesystem.XidImpl;

public interface XAFileSystem {

    public Session createSessionForLocalTransaction();

    public Session createSessionForXATransaction(Xid xid);

    public Session getSessionForTransaction(Xid xid);

    public void waitForBootup(long timeout) throws InterruptedException;

    public void notifySystemFailureAndContinue(Throwable t);

    public int getDefaultTransactionTimeout();

    public Xid[] recover(int flag) throws XAException;

    public void registerEndPointActivation(EndPointActivation epActivation);

    public void deRegisterEndPointActivation(EndPointActivation epActivation);

    public void shutdown() throws IOException;
}

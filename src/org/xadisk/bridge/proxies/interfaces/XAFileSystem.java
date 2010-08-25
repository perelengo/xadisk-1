package org.xadisk.bridge.proxies.interfaces;

import java.io.IOException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.connector.inbound.EndPointActivation;

/**
 * This interface is used only in standalone Java applications, and most of these methods are
 * for internal use only.
 */
public interface XAFileSystem {

    /**
     * Creates a new Session.
     * @return The new Session.
     */
    public Session createSessionForLocalTransaction();

    /**
     * For internal use only.
     * @param xid
     * @return
     */
    public Session createSessionForXATransaction(Xid xid);

    /**
     * For internal use only.
     * @param xid
     * @return
     */
    public Session getSessionForTransaction(Xid xid);

    /**
     * Waits for this XADisk instance to complete its booting and become ready to use. The timeout
     * specifies the maximum amount of time to wait; if timeout expires an appropriate exception is
     * thrown indicating the reason of why the booting hasn't yet completed.
     * @param timeout Number of milliseconds to wait.
     * @throws InterruptedException
     */
    public void waitForBootup(long timeout) throws InterruptedException;

    /**
     * For internal use only.
     * @param t
     */
    public void notifySystemFailureAndContinue(Throwable t);

    /**
     * For internal use only.
     * @return
     */
    public int getDefaultTransactionTimeout();

    /**
     * For internal use only.
     * @param flag
     * @return
     * @throws XAException
     */
    public Xid[] recover(int flag) throws XAException;

    /**
     * For internal use only.
     * @param epActivation
     */
    public void registerEndPointActivation(EndPointActivation epActivation) throws IOException;

    /**
     * For internal use only.
     * @param epActivation
     */
    public void deRegisterEndPointActivation(EndPointActivation epActivation) throws IOException;

    /**
     * If this is a NativeXAFileSystem object, this method shuts down the XADisk instance.
     * If this is a remote proxy for some XADisk instance, this method only disconnects from
     * the remote XADisk instance.
     * @throws IOException
     */
    public void shutdown() throws IOException;

    /**
     * For internal use only.
     * @return
     */
    public XAResource getEventProcessingXAResourceForRecovery();
}

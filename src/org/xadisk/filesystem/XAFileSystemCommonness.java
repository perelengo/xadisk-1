/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.filesystem;

import java.io.IOException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.connector.inbound.EndPointActivation;

public interface XAFileSystemCommonness extends XAFileSystem {

    public Session createSessionForXATransaction(Xid xid);

    public Session getSessionForTransaction(Xid xid);

    public void notifySystemFailureAndContinue(Throwable t);

    public int getDefaultTransactionTimeout();

    public Xid[] recover(int flag) throws XAException;

    public XAResource getEventProcessingXAResourceForRecovery();

    public void registerEndPointActivation(EndPointActivation epActivation) throws IOException;

    public void deRegisterEndPointActivation(EndPointActivation epActivation) throws IOException;
}

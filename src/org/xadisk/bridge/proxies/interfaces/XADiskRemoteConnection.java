package org.xadisk.bridge.proxies.interfaces;

import org.xadisk.connector.outbound.XADiskConnection;

/**
 * This interface is just a place holder for connections to remote XADisk instances (and was created
 * only to overcome a restriction from JCA specification). You should
 * write your JavaEE applications using the XADiskConnection interface and not this one.
 */
public interface XADiskRemoteConnection extends XADiskConnection {
}

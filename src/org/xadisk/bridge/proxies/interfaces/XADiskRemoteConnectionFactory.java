package org.xadisk.bridge.proxies.interfaces;

import org.xadisk.connector.outbound.XADiskConnectionFactory;

/**
 * This interface is just a place holder for connection factories to remote XADisk instances (and was created
 * only to overcome a restriction from JCA specification). You should
 * write your JavaEE applications using the XADiskConnectionFactory interface and not this one.
 */
public interface XADiskRemoteConnectionFactory extends XADiskConnectionFactory {
}

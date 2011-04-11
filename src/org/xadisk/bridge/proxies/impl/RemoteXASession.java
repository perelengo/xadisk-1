package org.xadisk.bridge.proxies.impl;

import org.xadisk.filesystem.NativeXASession;

public class RemoteXASession extends NativeXASession {

    public RemoteXASession(RemoteXAFileSystem remoteXAFileSystem) {
        super(remoteXAFileSystem, "dummy-value");
    }
}

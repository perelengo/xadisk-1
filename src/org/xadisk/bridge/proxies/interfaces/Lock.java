package org.xadisk.bridge.proxies.interfaces;

import java.io.File;
import java.io.Serializable;

public interface Lock extends Serializable {

    public File getResource();

    public boolean isExclusive();
}

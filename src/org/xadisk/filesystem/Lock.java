package org.xadisk.filesystem;

import java.io.File;
import java.io.Serializable;

public interface Lock extends Serializable {

    public File getResource();

    public boolean isExclusive();
}

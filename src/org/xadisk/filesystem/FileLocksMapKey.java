/*
Copyright © 2010-2011 Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */

package org.xadisk.filesystem;

import java.io.File;

public class FileLocksMapKey extends File {

    private final File path;
    private final boolean equalsAllPeerFiles; //files with same parent directory.

    public FileLocksMapKey(File path, boolean equalsAllPeerFiles) {
        super(path.getAbsolutePath());
        this.path = path;
        this.equalsAllPeerFiles = equalsAllPeerFiles;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FileLocksMapKey) {
            FileLocksMapKey that = (FileLocksMapKey) obj;
            if (that.path.equals(this.path)) {
                return true;
            }
            if (that.equalsAllPeerFiles) {
                return this.path.getParentFile().equals(that.path.getParentFile());
            }
            if (this.equalsAllPeerFiles) {
                return that.path.getParentFile().equals(this.path.getParentFile());
            }
            return false;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return path.getParentFile().hashCode();
    }
}

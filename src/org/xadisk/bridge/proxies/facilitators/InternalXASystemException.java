/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.bridge.proxies.facilitators;

import org.xadisk.filesystem.exceptions.XASystemException;

public class InternalXASystemException extends XASystemException {

    public InternalXASystemException(Throwable cause) {
        super(cause);
    }
}

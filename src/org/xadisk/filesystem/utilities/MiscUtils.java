/*
Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/

package org.xadisk.filesystem.utilities;

import javax.transaction.xa.XAException;

public class MiscUtils {

    public static XAException createXAExceptionWithCause(int errorCode, Throwable cause) {
        XAException xae = new XAException(errorCode);
        xae.initCause(cause);
        return xae;
    }
}

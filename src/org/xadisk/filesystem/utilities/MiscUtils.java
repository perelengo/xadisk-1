package org.xadisk.filesystem.utilities;

import javax.transaction.xa.XAException;

public class MiscUtils {

    public static XAException createXAExceptionWithCause(int errorCode, Throwable cause) {
        XAException xae = new XAException(errorCode);
        xae.initCause(cause);
        return xae;
    }
}

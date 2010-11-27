package org.xadisk.connector.outbound;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionMetaData;

public class XADiskManagedConnectionMetaData implements ManagedConnectionMetaData {

    public String getEISProductName() throws ResourceException {
        return "XADisk";
    }

    public String getEISProductVersion() throws ResourceException {
        return "1.0";
    }

    public int getMaxConnections() throws ResourceException {
        return 0;
    }

    public String getUserName() throws ResourceException {
        return "irrelevant";
    }
}

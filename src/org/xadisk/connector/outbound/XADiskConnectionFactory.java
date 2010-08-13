package org.xadisk.connector.outbound;

import java.io.Serializable;
import javax.resource.Referenceable;
import javax.resource.ResourceException;

public interface XADiskConnectionFactory extends Serializable, Referenceable {

    public XADiskConnection getConnection() throws ResourceException;
}

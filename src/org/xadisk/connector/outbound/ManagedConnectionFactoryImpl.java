package org.xadisk.connector.outbound;

import java.io.PrintWriter;
import java.util.Set;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

public class ManagedConnectionFactoryImpl implements ManagedConnectionFactory {

    private volatile PrintWriter logWriter;

    public ManagedConnectionFactoryImpl() {
        
    }

    public Object createConnectionFactory() throws ResourceException {
        throw new NotSupportedException("Works only in managed environments.");
    }

    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new ConnectionFactory(this, cm);
    }

    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        return new ManagedConnectionImpl();
    }

    public ManagedConnection matchManagedConnections(Set candidates, Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        boolean glassFish = false;
        /*there was a bug with glassfish-2, throwing NSException was strange for the container.
        But, with glassfish301, none of the following works (contrary to JCA spec). So, only workaround for
        developers is to disable connection pooling when working with glassfish.*/
        if (glassFish) {
            System.out.println("Please disable pooling for the XADisk connection pool.");
            return null;
        } else {
            System.out.println("Please disable pooling for the XADisk connection pool.");
            throw new NotSupportedException("Please don't pool connections to this EIS");
        }
    }

    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    public void setLogWriter(PrintWriter logWriter) throws ResourceException {
        this.logWriter = logWriter;
    }

    @Override
    public int hashCode() {
        return 101;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ManagedConnectionFactoryImpl) {
            ManagedConnectionFactoryImpl mcf = (ManagedConnectionFactoryImpl) obj;
            return true;
        }
        return false;
    }
}

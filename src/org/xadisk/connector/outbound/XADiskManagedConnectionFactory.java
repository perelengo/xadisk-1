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

public class XADiskManagedConnectionFactory implements ManagedConnectionFactory {

    private volatile PrintWriter logWriter;

    public XADiskManagedConnectionFactory() {
    }

    public Object createConnectionFactory() throws ResourceException {
        throw new NotSupportedException("Works only in managed environments.");
    }

    public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
        return new XADiskConnectionFactoryImpl(this, cm);
    }

    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        return new XADiskManagedConnection();
    }

    public ManagedConnection matchManagedConnections(Set candidates, Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        boolean glassFish = false;
        /*Throwing NSE doesn't work for glassfish and JBoss atleast. So, one workaround for
        developers was to disable connection pooling when working with glassfish. But JBoss doeesn't
        seem to have an option for disabling pooling. Looking
        broadly, not "all" j2ee server implementations may support disabling of pooling; so i am
        now implementing a hack "inside". I will return the first connection blindly for "local connection"
        cases, and for remote connections, no need to check for address/port as an MCF will get only
        those in the set which are from that MCF itself (i trust).
         */
        //throw new NotSupportedException("Please don't pool connections to this EIS");
        if (candidates.size() == 0) {
            return null;
        }
        Object mc = candidates.iterator().next();
        if (mc instanceof XADiskManagedConnection) {
            return (XADiskManagedConnection) mc;
        }
        return null;
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
        if (obj instanceof XADiskManagedConnectionFactory) {
            XADiskManagedConnectionFactory mcf = (XADiskManagedConnectionFactory) obj;
            return true;
        }
        return false;
    }
}

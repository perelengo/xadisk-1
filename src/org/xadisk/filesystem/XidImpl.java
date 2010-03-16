package org.xadisk.filesystem;

import java.nio.ByteBuffer;
import javax.transaction.xa.Xid;
import org.xadisk.filesystem.ResourceDependencyGraph.Node;

public class XidImpl implements Xid {

    private final byte[] gid;
    private final byte[] bqual;
    private final int formatId;
    private volatile ResourceDependencyGraph.Node nodeInResourceDependencyGraph = null;
    private volatile byte interruptCause = 0;
    public static final byte INTERRUPTED_DUE_TO_DEADLOCK = 1;
    public static final byte INTERRUPTED_DUE_TO_TIMEOUT = 2;
    public final Object interruptFlagLock = new Object();
    private Session owningSession;

    XidImpl(ByteBuffer buffer) {
        int gidLength = buffer.get();
        int bqualLength = buffer.get();
        this.formatId = buffer.getInt();
        this.gid = new byte[gidLength];
        this.bqual = new byte[bqualLength];
        buffer.get(gid);
        buffer.get(bqual);
    }

    public XidImpl(Xid xid) {
        gid = xid.getGlobalTransactionId();
        bqual = xid.getBranchQualifier();
        formatId = xid.getFormatId();
    }

    @Override
    public byte[] getBranchQualifier() {
        return bqual;
    }

    @Override
    public int getFormatId() {
        return formatId;
    }

    @Override
    public byte[] getGlobalTransactionId() {
        return gid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof XidImpl) {
            XidImpl xid = (XidImpl) obj;
            if (xid.getFormatId() != formatId) {
                return false;
            }
            byte temp[] = xid.getGlobalTransactionId();
            for (int i = 0; i < temp.length; i++) {
                if (i >= gid.length || temp[i] != gid[i]) {
                    return false;
                }
            }
            temp = xid.getBranchQualifier();
            for (int i = 0; i < temp.length; i++) {
                if (i >= bqual.length || temp[i] != bqual[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        if (gid.length > 0) {
            hashCode += gid[0] + gid[gid.length / 2] + gid[gid.length - 1];
        }
        if (bqual.length > 0) {
            hashCode += bqual[0] + bqual[bqual.length / 2] + bqual[bqual.length - 1];
        }
        return hashCode;
    }

    public Node getNodeInResourceDependencyGraph() {
        return nodeInResourceDependencyGraph;
    }

    public void setNodeInResourceDependencyGraph(Node nodeInResourceDependencyGraph) {
        this.nodeInResourceDependencyGraph = nodeInResourceDependencyGraph;
    }

    public byte getInterruptCause() {
        return interruptCause;
    }

    public void setInterruptCause(byte interruptCause) {
        this.interruptCause = interruptCause;
    }

    public static XidImpl getXidInstanceForLocalTransaction(long localTransactionId) {
        ByteBuffer tidBuffer = ByteBuffer.allocate(20);
        tidBuffer.put((byte) 8);
        tidBuffer.put((byte) 0);
        tidBuffer.putInt(101);
        tidBuffer.putLong(localTransactionId);
        tidBuffer.flip();
        return new XidImpl(tidBuffer);
    }

    public Session getOwningSession() {
        return owningSession;
    }

    public void setOwningSession(Session owningSession) {
        this.owningSession = owningSession;
    }
}

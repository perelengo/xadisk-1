package org.xadisk.filesystem;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

class ResourceDependencyGraph {

    private final ConcurrentHashMap<XidImpl, Node> nodes = new ConcurrentHashMap<XidImpl, Node>(1000);

    ResourceDependencyGraph() {
    }

    void addDependency(XidImpl dependent, Lock resource) {
        Node source = dependent.getNodeInResourceDependencyGraph();
        source.setResourceWaitingFor(resource);
    }

    void removeDependency(XidImpl dependent) {
        Node source = dependent.getNodeInResourceDependencyGraph();
        source.setResourceWaitingFor(null);
    }

    void removeNodeForTransaction(XidImpl xid) {
        nodes.remove(xid);
        xid.setNodeInResourceDependencyGraph(null);
    }

    void createNodeForTransaction(XidImpl xid) {
        Node node = new Node(xid, 0, Thread.currentThread());
        nodes.put(xid, node);
        xid.setNodeInResourceDependencyGraph(node);
    }

    Node[] getNodes() {
        return nodes.values().toArray(new Node[0]);
    }

    /*public Node generateNodeForTesting() {
    Node n = new Node(nodes.size(), 0);
    nodes.put(XidImpl.getXidInstanceForLocalTransaction(nodes.size()), n);
    return n;
    }*/
    
    class Node {

        private final Object id;
        private final ArrayList<Node> neighbors = new ArrayList<Node>(10);
        private int mark;
        private int prepostVisit[] = new int[2];
        private Node parent = null;
        private int nextNeighborToProcess = 0;
        private final Thread transactionThread;
        private volatile Lock resourceWaitingFor = null;

        private Node(Object id, int defaultMark, Thread transactionThread) {
            this.id = id;
            this.mark = defaultMark;
            this.transactionThread = transactionThread;
        }

        void addNeighbor(Node n) {
            neighbors.add(n);
        }
        
        ArrayList<Node> getNeighbors() {
            return neighbors;
        }

        void setMark(int mark) {
            this.mark = mark;
        }

        int getMark() {
            return mark;
        }

        void setPreVisit(int preVisit) {
            prepostVisit[0] = preVisit;
        }

        void setPostVisit(int postVisit) {
            prepostVisit[1] = postVisit;
        }

        int[] getPrepostVisit() {
            return prepostVisit;
        }

        Node getParent() {
            return parent;
        }

        void setParent(Node parent) {
            this.parent = parent;
        }

        Object getId() {
            return id;
        }

        void resetAlgorithmicData() {
            mark = 0;
            parent = null;
            prepostVisit[0] = 0;
            prepostVisit[1] = 0;
            nextNeighborToProcess = 0;
            neighbors.clear();
        }

        boolean isWaitingForResource() {
            return resourceWaitingFor != null;
        }

        int getNextNeighborToProcess() {
            return nextNeighborToProcess;
        }

        void forwardNextNeighborToProcess() {
            nextNeighborToProcess++;
        }

        Thread getTransactionThread() {
            return transactionThread;
        }

        void setResourceWaitingFor(Lock resourceWaitingFor) {
            this.resourceWaitingFor = resourceWaitingFor;
        }

        Lock getResourceWaitingFor() {
            return resourceWaitingFor;
        }
    }
}

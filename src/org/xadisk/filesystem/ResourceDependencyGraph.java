/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.filesystem;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceDependencyGraph {

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

    public Node[] getNodes() {
        return nodes.values().toArray(new Node[nodes.size()]);
    }

    /*public Node generateNodeForTesting() {
    Node n = new Node(nodes.size(), 0);
    nodes.put(XidImpl.getXidInstanceForLocalTransaction(nodes.size()), n);
    return n;
    }*/
    
    public static class Node {

        private final Object id;
        private final ArrayList<Node> neighbors = new ArrayList<Node>(10);
        private int mark;
        private int prepostVisit[] = new int[2];
        private Node parent = null;
        private int nextNeighborToProcess = 0;
        private volatile Thread transactionThread;
        private volatile Lock resourceWaitingFor = null;

        private Node(Object id, int defaultMark, Thread transactionThread) {
            this.id = id;
            this.mark = defaultMark;
            this.transactionThread = transactionThread;
        }

        public void reAssociatedTransactionThread(Thread transactionThread) {
            this.transactionThread = transactionThread;
        }

        public void addNeighbor(Node n) {
            neighbors.add(n);
        }
        
        public ArrayList<Node> getNeighbors() {
            return neighbors;
        }

        public void setMark(int mark) {
            this.mark = mark;
        }

        public int getMark() {
            return mark;
        }

        public void setPreVisit(int preVisit) {
            prepostVisit[0] = preVisit;
        }

        public void setPostVisit(int postVisit) {
            prepostVisit[1] = postVisit;
        }

        public int[] getPrepostVisit() {
            return prepostVisit;
        }

        public Node getParent() {
            return parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public Object getId() {
            return id;
        }

        public void resetAlgorithmicData() {
            mark = 0;
            parent = null;
            prepostVisit[0] = 0;
            prepostVisit[1] = 0;
            nextNeighborToProcess = 0;
            neighbors.clear();
        }

        public boolean isWaitingForResource() {
            return resourceWaitingFor != null;
        }

        public int getNextNeighborToProcess() {
            return nextNeighborToProcess;
        }

        public void forwardNextNeighborToProcess() {
            nextNeighborToProcess++;
        }

        Thread getTransactionThread() {
            return transactionThread;
        }

        void setResourceWaitingFor(Lock resourceWaitingFor) {
            this.resourceWaitingFor = resourceWaitingFor;
        }

        public Lock getResourceWaitingFor() {
            return resourceWaitingFor;
        }
    }
}

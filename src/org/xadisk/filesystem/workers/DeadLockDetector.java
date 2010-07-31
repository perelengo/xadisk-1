package org.xadisk.filesystem.workers;

import java.util.ArrayList;
import java.util.Stack;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.Lock;
import org.xadisk.filesystem.ResourceDependencyGraph;
import org.xadisk.filesystem.XidImpl;

public class DeadLockDetector extends TimedWorker {

    private final NativeXAFileSystem xaFileSystem;
    private final ResourceDependencyGraph rdg;
    private ResourceDependencyGraph.Node[] nodes = new ResourceDependencyGraph.Node[0];
    private final ArrayList<ResourceDependencyGraph.Node> backEdges = new ArrayList<ResourceDependencyGraph.Node>(10);

    public DeadLockDetector(int frequency, ResourceDependencyGraph rdg, NativeXAFileSystem xaFileSystem) {
        super(frequency);
        this.rdg = rdg;
        this.xaFileSystem = xaFileSystem;
    }

    @Override
    void doWorkOnce() {
        try {
            while (true) {
                cleanup();
                takeSnapShotOfRDG();
                runDFS();
                if (backEdges.size() == 0) {
                    break;
                }
                breakCycles();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } catch (Throwable t) {
            xaFileSystem.notifySystemFailure(t);
        }
    }

    private void takeSnapShotOfRDG() {
        nodes = rdg.getNodes();
        for (int i = 0; i < nodes.length; i++) {
            ResourceDependencyGraph.Node node = nodes[i];
            Lock resource = node.getResourceWaitingFor();
            if (resource == null) {
                continue;
            }
            XidImpl holders[];
            try {
                resource.startSynchBlock();
                holders = resource.getHolders().toArray(new XidImpl[0]);
            } finally {
                resource.endSynchBlock();
            }
            for (int j = 0; j < holders.length; j++) {
                ResourceDependencyGraph.Node neighbor = holders[j].getNodeInResourceDependencyGraph();
                if (neighbor != null && neighbor != node)
                {
                    node.addNeighbor(neighbor);
                }
            }
        }
    }

    private void runDFS() {
        int clock = 0;
        Stack nodesBeingExplored = new Stack();
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].setMark(1);
        }

        for (int i = 0; i < nodes.length; i++) {
            ResourceDependencyGraph.Node nodeBeingExplored = nodes[i];
            if (nodeBeingExplored.getMark() == 2) {
                continue;
            }
            nodesBeingExplored.push(nodeBeingExplored);
            while (!nodesBeingExplored.empty()) {
                nodeBeingExplored = (ResourceDependencyGraph.Node) nodesBeingExplored.peek();
                if (nodeBeingExplored.getMark() != 2) {
                    nodeBeingExplored.setMark(2);
                    nodeBeingExplored.setPreVisit(clock++);
                }
                ArrayList<ResourceDependencyGraph.Node> neighbors = nodeBeingExplored.getNeighbors();
                boolean exploredCompletely = true;
                for (int j = nodeBeingExplored.getNextNeighborToProcess(); j < neighbors.size(); j++) {
                    nodeBeingExplored.forwardNextNeighborToProcess();
                    ResourceDependencyGraph.Node nextToExplore = neighbors.get(j);
                    if (nextToExplore.getMark() == 2) {
                        detectBackEdge(nodeBeingExplored, nextToExplore);
                        continue;
                    }
                    nodesBeingExplored.push(nextToExplore);
                    nextToExplore.setParent(nodeBeingExplored);
                    exploredCompletely = false;
                    break;
                }
                if (exploredCompletely) {
                    nodesBeingExplored.pop();
                    nodeBeingExplored.setPostVisit(clock++);
                }
            }
        }
    }

    private void detectBackEdge(ResourceDependencyGraph.Node source, ResourceDependencyGraph.Node target) {
        if (target.getPrepostVisit()[0] < source.getPrepostVisit()[0] &&
                target.getPrepostVisit()[1] == 0) {
            backEdges.add(source);
            backEdges.add(target);
            return;
        }
    }

    private void breakCycles() {
        for (int i = 0; i < backEdges.size() - 1; i += 2) {
            ResourceDependencyGraph.Node source = backEdges.get(i);
            ResourceDependencyGraph.Node target = backEdges.get(i + 1);
            ResourceDependencyGraph.Node victim = null;
            int minimumLocks = -1;
            while (source != target) {
                if (!source.isWaitingForResource()) {
                    break;
                }
                int currentLocksCount = ((XidImpl) source.getId()).getOwningSession().getNumOwnedExclusiveLocks();
                if (minimumLocks == -1) {
                    victim = source;
                    minimumLocks = currentLocksCount;
                } else if (currentLocksCount < minimumLocks) {
                    minimumLocks = currentLocksCount;
                    victim = source;
                }
                source = source.getParent();
            }

            if (victim != null && victim.isWaitingForResource()) {
                XidImpl victimXid = (XidImpl) victim.getId();
                xaFileSystem.interruptTransactionIfWaitingForResourceLock(victimXid, XidImpl.INTERRUPTED_DUE_TO_DEADLOCK);
            }
        }
    }

    private void cleanup() {
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].resetAlgorithmicData();
        }
        backEdges.clear();
    }
}

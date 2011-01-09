/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.tests.correctness;

import java.util.ArrayList;
import java.util.Stack;

public class DFSTest {
/*
    static ArrayList<ResourceDependencyGraph.Node> backEdges = new ArrayList<ResourceDependencyGraph.Node>();
    static ResourceDependencyGraph rdg = new ResourceDependencyGraph();

    public static void main(String args[]) {
        ResourceDependencyGraph.Node[] nodes = new ResourceDependencyGraph.Node[5];
        for (int i = 0; i < 5; i++) {
            nodes[i] = rdg.generateNodeForTesting();
        }
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                if (i != j && i%2==1) {
                    nodes[i].addNeighbor(nodes[j]);
                }
            }
        }

        runDFS();

        printDFSTree(nodes);

        printBackEdges();

        breakCycles();
    }

    private static void printDFSTree(ResourceDependencyGraph.Node[] nodes) {
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i].getParent() != null) {
                System.out.println(nodes[i].getId() + " has parent " + nodes[i].getParent().getId());
            } else {
                System.out.println(nodes[i].getId() + " is the ROOT");
            }
        }
    }

    private static void printBackEdges() {
        for (int i = 0; i < backEdges.size() - 1; i += 2) {
            ResourceDependencyGraph.Node source = backEdges.get(i);
            ResourceDependencyGraph.Node target = backEdges.get(i + 1);
            System.out.println("BackEdge : from node " + source.getId() + " to " + target.getId());
        }
    }

    private static void runDFS() {
        int clock = 0;
        Stack nodesBeingExplored = new Stack();
        ResourceDependencyGraph.Node nodes[] = rdg.getNodes();
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
                ResourceDependencyGraph.Node[] neighbors = nodeBeingExplored.getNeighbors();
                boolean exploredCompletely = true;
                for (int j = nodeBeingExplored.getNextNeighborToProcess(); j < neighbors.length; j++) {
                    nodeBeingExplored.forwardNextNeighborToProcess();
                    ResourceDependencyGraph.Node nextToExplore = neighbors[j];
                    if (!nextToExplore.isWaitingForResource()) {
                        nodeBeingExplored.removeNeighbor(nextToExplore);
                        continue;
                    }
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

    private static void detectBackEdge(ResourceDependencyGraph.Node source, ResourceDependencyGraph.Node target) {
        if (target.getPrepostVisit()[0] < source.getPrepostVisit()[0] &&
                target.getPrepostVisit()[1] == 0) {
            backEdges.add(source);
            backEdges.add(target);
            return;
        }
    }

    private static void breakCycles() {
        ResourceDependencyGraph.Node backEdgeNodes[] = backEdges.toArray(new ResourceDependencyGraph.Node[0]);
        for (int i = 0; i < backEdgeNodes.length - 1; i += 2) {
            ResourceDependencyGraph.Node source = backEdgeNodes[i];
            ResourceDependencyGraph.Node target = backEdgeNodes[i + 1];
            ResourceDependencyGraph.Node victim = null;
            int minimumLocks = -1;
            ResourceDependencyGraph.Node temp = source;
            minimumLocks = temp.hashCode();
            victim = temp;
            temp = source.getParent();
            while (source != target) {
                int currentLocksCount = temp.hashCode();
                if (currentLocksCount < minimumLocks) {
                    minimumLocks = currentLocksCount;
                    victim = temp;
                }
                source = source.getParent();
            }
            System.out.println("Identified one victim of deadlock : " + temp.getId());
        }
    } */
}
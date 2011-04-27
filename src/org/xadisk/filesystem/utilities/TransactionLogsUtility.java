/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.filesystem.utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import org.xadisk.filesystem.DurableDiskSession;
import org.xadisk.filesystem.XidImpl;

public class TransactionLogsUtility {

    public static void addLogPositionToTransaction(XidImpl xid, int logFileIndex,
            long localPosition, Map<XidImpl, ArrayList<Long>> transactionLogPositions) {
        ArrayList<Long> temp = transactionLogPositions.get(xid);
        if (temp == null) {
            temp = new ArrayList<Long>(25);
            transactionLogPositions.put(xid, temp);
        }
        temp.add((long) logFileIndex);
        temp.add(localPosition);
    }

    public static void deleteLogsIfPossible(XidImpl xid, Map<XidImpl, ArrayList<Integer>> transactionsAndLogsOccupied,
            Map<Integer, Integer> transactionLogsAndOpenTransactions, int currentLogIndex,
            String transactionLogBaseName, DurableDiskSession durableDiskSession) throws IOException {
        ArrayList<Integer> logsOccupied = transactionsAndLogsOccupied.get(xid);
        if (logsOccupied == null) {
            return;
        }
        for (Integer logFileIndex : logsOccupied) {
            Integer numTxns = transactionLogsAndOpenTransactions.get(logFileIndex);
            numTxns--;
            if (numTxns == 0 && currentLogIndex != logFileIndex) {
                durableDiskSession.deleteFileDurably(new File(transactionLogBaseName + "_" + logFileIndex));
            } else {
                transactionLogsAndOpenTransactions.put(logFileIndex, numTxns);
            }
        }
    }

    public static void trackTransactionLogsUsage(XidImpl xid, Map<XidImpl, ArrayList<Integer>> transactionsAndLogsOccupied,
            Map<Integer, Integer> transactionLogsAndOpenTransactions, int logFileIndex) {
        boolean txnFirstTimeInThisLog = false;
        ArrayList<Integer> logsOccupied = transactionsAndLogsOccupied.get(xid);
        if (logsOccupied == null) {
            logsOccupied = new ArrayList<Integer>(2);
            transactionsAndLogsOccupied.put(xid, logsOccupied);
        }
        if (!logsOccupied.contains(logFileIndex)) {
            logsOccupied.add(logFileIndex);
            txnFirstTimeInThisLog = true;
        }
        if (txnFirstTimeInThisLog) {
            Integer numTxns = transactionLogsAndOpenTransactions.get(logFileIndex);
            if (numTxns == null) {
                numTxns = Integer.valueOf(0);
            }
            numTxns++;
            transactionLogsAndOpenTransactions.put(logFileIndex, numTxns);
        }
    }
}

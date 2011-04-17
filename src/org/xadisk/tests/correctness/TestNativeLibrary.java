package org.xadisk.tests.correctness;

import org.xadisk.filesystem.DurableDiskSession;

public class TestNativeLibrary {

    public static void main(String args[]) {
        try {
            DurableDiskSession.testNativeLibrary();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

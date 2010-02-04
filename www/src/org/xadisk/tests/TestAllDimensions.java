package org.xadisk.tests;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector.StringArgument;
import com.sun.jdi.connect.LaunchingConnector;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import org.xadisk.filesystem.StandaloneFileSystemConfiguration;
import org.xadisk.filesystem.XAFileSystem;
import org.xadisk.filesystem.exceptions.XASystemException;

public class TestAllDimensions {
    private static final String SEPERATOR = File.separator;
    private static final String currentWorkingDirectory = System.getProperty("user.dir");
    private static final String topLevelTestDirectory = currentWorkingDirectory + SEPERATOR + "testXADisk";
    private static final String XADiskSystemDirectory = currentWorkingDirectory + SEPERATOR + "XADisk";
    private static boolean testCrashRecovery = false;
    public static int concurrencyLevel = 10;
    private static int numberOfCrashes = 100;
    public static int maxConcurrentDeliveries = 2;
    private static final String forRunningTests = "forRunningTests";
    private static final String transactionDemarcatingThread = "TestThreadObservedByJDI";

    public static void main(String args[]) {
        try {
            if (args.length > 0 && args[0].equals(forRunningTests)) {
                System.out.println("Entered into the main of childJVM " + forRunningTests);
                test(false);
                System.out.println("Exit...");
            } else {
                if (testCrashRecovery) {
                    System.out.println("_____________Start-CrashRecoveryTests______________");
                    for (int i = 1; i <= numberOfCrashes; i++) {
                        TestUtility.cleanupDirectory(new File(XADiskSystemDirectory));
                        TestUtility.cleanupDirectory(new File(topLevelTestDirectory));
                        System.out.println("Raising child JVM for controlled crash...");
                        Process controlledJVM = powerOnJVMAsDebugee(forRunningTests, i);
                        int status = controlledJVM.waitFor();
                        if (status == 0) {
                            break;
                        }
                        System.out.println("Crashed!! Status=" + status);
                        test(true);
                        System.out.println("_______________Recovered Successfully______________");
                    }
                } else {
                    TestUtility.cleanupDirectory(new File(XADiskSystemDirectory));
                    TestUtility.cleanupDirectory(new File(topLevelTestDirectory));
                    test(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Process powerOnJVMAsDebugee(String purpose, int crashAfterBreakpoint)
            throws Exception {
        LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
        Map connectorArguments = connector.defaultArguments();
        StringArgument mainArgument = (StringArgument) connectorArguments.get("main");
        mainArgument.setValue(org.xadisk.tests.TestAllDimensions.class.getName() + " " + purpose);
        StringArgument optionsArgument = (StringArgument) connectorArguments.get("options");
        optionsArgument.setValue("-classpath build" + SEPERATOR + "classes;" +
                "dist" + SEPERATOR + "lib" + SEPERATOR + "javaee.jar ");
        VirtualMachine vm = connector.launch(connectorArguments);
        new Thread(new JDIEventsHandler(vm, transactionDemarcatingThread, crashAfterBreakpoint)).start();

        Process jvmProcess = vm.process();
        new Thread(new ChildProcessOutputStreamSpacer(jvmProcess.getErrorStream())).start();
        new Thread(new ChildProcessOutputStreamSpacer(jvmProcess.getInputStream())).start();

        return jvmProcess;
    }

    private static void test(boolean postCrash) {
        try {
            StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(XADiskSystemDirectory);
            configuration.setWorkManagerCorePoolSize(100);
            configuration.setWorkManagerMaxPoolSize(100);
            configuration.setMaximumConcurrentEventDeliveries(maxConcurrentDeliveries);
            XAFileSystem.bootXAFileSystemStandAlone(configuration);

            while (true) {
                try {
                    Thread.sleep(1000);
                    XAFileSystem.getXAFileSystem().checkIfCanContinue();
                } catch (XASystemException xase) {
                    System.out.println("Still Recovering...");
                    xase.printStackTrace();
                    continue;
                }
                break;
            }
            System.out.println("Recovery over.");
            Class.forName(org.xadisk.filesystem.Session.class.getCanonicalName());
            Class.forName(org.xadisk.filesystem.GatheringDiskWriter.class.getName());
            ArrayList<Thread> allThreads = new ArrayList<Thread>();
            Thread tests[] = new Thread[4];
            for (int testReplica = 1; testReplica <= concurrencyLevel; testReplica++) {
                if (postCrash) {
                    tests[0] = new Thread(new TestRunnable(InspiredDiskIOs.testConcurrentMoneyTransferPostCrash,
                            topLevelTestDirectory + SEPERATOR + "moneyTransfer" + testReplica));
                    tests[1] = new Thread(new TestRunnable(InspiredDiskIOs.testDynamicReadWritePostCrash,
                            topLevelTestDirectory + SEPERATOR + "dynamicReadWrite" + testReplica));
                    tests[2] = new Thread(new TestRunnable(InspiredDiskIOs.testIOOperationsPostCrash,
                            topLevelTestDirectory + SEPERATOR + "ioOperations" + testReplica));
                    tests[3] = new Thread(new TestRunnable(InspiredDiskIOs.testFileSystemEventingPostCrash,
                            topLevelTestDirectory + SEPERATOR + "eventingSystems" + testReplica));
                } else {
                    tests[0] = new Thread(new TestRunnable(InspiredDiskIOs.testConcurrentMoneyTransfer,
                            topLevelTestDirectory + SEPERATOR + "moneyTransfer" + testReplica));
                    tests[1] = new Thread(new TestRunnable(InspiredDiskIOs.testDynamicReadWrite,
                            topLevelTestDirectory + SEPERATOR + "dynamicReadWrite" + testReplica));
                    tests[2] = new Thread(new TestRunnable(InspiredDiskIOs.testIOOperations,
                            topLevelTestDirectory + SEPERATOR + "ioOperations" + testReplica));
                    tests[3] = new Thread(new TestRunnable(InspiredDiskIOs.testFileSystemEventing,
                            topLevelTestDirectory + SEPERATOR + "eventingSystem" + testReplica));
                }
                for (int i = 0; i < 4; i++) {
                    if (i == 2) {
                        tests[i].setName(transactionDemarcatingThread);
                        tests[i].start();
                        allThreads.add(tests[i]);
                    }
                }
            }
            TestUtility.waitForAllAtHeaven(allThreads);
            XAFileSystem.getXAFileSystem().shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

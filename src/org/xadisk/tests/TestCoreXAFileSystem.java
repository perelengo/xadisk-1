package org.xadisk.tests;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector.StringArgument;
import com.sun.jdi.connect.LaunchingConnector;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

public class TestCoreXAFileSystem {

    private static final String SEPERATOR = File.separator;
    private static final String currentWorkingDirectory = System.getProperty("user.dir");
    private static final String topLevelTestDirectory = currentWorkingDirectory + SEPERATOR + "testXADiskSystem1";
    private static final String XADiskSystemDirectory = currentWorkingDirectory + SEPERATOR + "XADiskSystem";
    private static boolean testCrashRecovery = false;
    public static int concurrencyLevel = 1;
    private static int numberOfCrashes = 100;
    public static int maxConcurrentDeliveries = 2;
    private static final String forRunningTests = "forRunningTests";
    private static final String transactionDemarcatingThread = "TestThreadObservedByJDI";

    public static void main(String args[]) {
        try {
            CoreXAFileSystemTests.testHighNumber = false;
            CoreXAFileSystemTests.testProgressive = false;
            CoreXAFileSystemTests.usePessimisticLock = true;
            TestUtility.remoteXAFileSystem = true;

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
                        Process controlledJVM = powerOnJVMAsDebugeeForCrashes(forRunningTests, i);
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

    private static VirtualMachine powerOnJVMAsDebugee(String purpose)
            throws Exception {
        LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
        Map connectorArguments = connector.defaultArguments();
        StringArgument mainArgument = (StringArgument) connectorArguments.get("main");
        mainArgument.setValue(org.xadisk.tests.TestCoreXAFileSystem.class.getName() + " " + purpose);
        StringArgument optionsArgument = (StringArgument) connectorArguments.get("options");
        optionsArgument.setValue("-classpath build" + SEPERATOR + "classes;"
                + "connector-api-1.5.jar;javaee-api-5.jar");
        return connector.launch(connectorArguments);
    }

    private static void initiateOutputProcessing(Process jvmProcess) {
        new Thread(new ChildProcessOutputStreamReader(jvmProcess.getErrorStream(),
                System.err)).start();
        new Thread(new ChildProcessOutputStreamReader(jvmProcess.getInputStream(),
                System.out)).start();
    }

    private static Process powerOnJVMAsDebugeeForCrashes(String purpose, int crashAfterBreakpoint)
            throws Exception {
        VirtualMachine vm = powerOnJVMAsDebugee(purpose);
        new Thread(new JVMCrashTrigger(vm, transactionDemarcatingThread, crashAfterBreakpoint)).start();
        Process jvmProcess = vm.process();
        initiateOutputProcessing(jvmProcess);
        return jvmProcess;
    }

    private static void test(boolean postCrash) {
        try {
            StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(XADiskSystemDirectory);
            configuration.setWorkManagerCorePoolSize(10);
            configuration.setWorkManagerMaxPoolSize(10000);
            configuration.setMaximumConcurrentEventDeliveries(maxConcurrentDeliveries);
            configuration.setTransactionTimeout(Integer.MAX_VALUE);
            configuration.setDeadLockDetectorInterval(2);
            configuration.setLockTimeOut(10 * 1000);
            configuration.setServerAddress("localhost");
            configuration.setServerPort(5151);
            NativeXAFileSystem nativeXAFileSystem = NativeXAFileSystem.bootXAFileSystemStandAlone(configuration);
            nativeXAFileSystem.waitForBootup(-1);

            System.out.println("Recovery over.");
            Class.forName(org.xadisk.filesystem.NativeSession.class.getCanonicalName());
            Class.forName(org.xadisk.filesystem.workers.GatheringDiskWriter.class.getName());
            ArrayList<Thread> allThreads = new ArrayList<Thread>();
            Thread tests[] = new Thread[4];
            for (int testReplica = 1; testReplica <= concurrencyLevel; testReplica++) {
                if (postCrash) {
                    tests[0] = new Thread(new RunnableTest(CoreXAFileSystemTests.testConcurrentMoneyTransferPostCrash,
                            topLevelTestDirectory + SEPERATOR + "moneyTransfer" + testReplica));
                    tests[1] = new Thread(new RunnableTest(CoreXAFileSystemTests.testDynamicReadWritePostCrash,
                            topLevelTestDirectory + SEPERATOR + "dynamicReadWrite" + testReplica));
                    tests[2] = new Thread(new RunnableTest(CoreXAFileSystemTests.testIOOperationsPostCrash,
                            topLevelTestDirectory + SEPERATOR + "ioOperations" + testReplica));
                    tests[3] = new Thread(new RunnableTest(CoreXAFileSystemTests.testFileSystemEventingPostCrash,
                            topLevelTestDirectory + SEPERATOR + "eventingSystems" + testReplica));
                } else {
                    tests[0] = new Thread(new RunnableTest(CoreXAFileSystemTests.testConcurrentMoneyTransfer,
                            topLevelTestDirectory + SEPERATOR + "moneyTransfer" + testReplica));
                    tests[1] = new Thread(new RunnableTest(CoreXAFileSystemTests.testDynamicReadWrite,
                            topLevelTestDirectory + SEPERATOR + "dynamicReadWrite" + testReplica));
                    tests[2] = new Thread(new RunnableTest(CoreXAFileSystemTests.testIOOperations,
                            topLevelTestDirectory + SEPERATOR + "ioOperations" + testReplica));
                    tests[3] = new Thread(new RunnableTest(CoreXAFileSystemTests.testFileSystemEventing,
                            topLevelTestDirectory + SEPERATOR + "eventingSystem" + testReplica));
                }
                for (int i = 0; i < 4; i++) {
                    if (i == 3) {
                        tests[i].setName(transactionDemarcatingThread);
                        tests[i].start();
                        allThreads.add(tests[i]);
                    }
                }
            }
            TestUtility.waitForAllAtHeaven(allThreads);
            System.out.println("Testing threads completed...Will shutdown the NativeXAFS now.");
            NativeXAFileSystem.getXAFileSystem().shutdown();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

package org.xadisk.examples.standalone;

import java.io.File;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

/*This is a very basic example to outline the usage of XADisk. It is applicable to only standalone Java applications, i.e.
those running in a non-managed environment.*/

/*
 * To compile/run this example, one needs:
 *      1. XADisk.jar, extracted from the XADisk.rar
 *      2. JCA API jar, which can be downloaded from http://download.java.net/maven/1/javax.resource/jars/connector-api-1.5.jar
 *      3. Java 5 or above.
 * Before running this sample, please change the local file-system paths used below according to your environment.
 */

/* This example is only intended to show a usage for XADisk. The other programming practices can vary among programmers.*/

public class Basic {

    public static void main(String args[]) {
        XAFileSystem xaf = null;
        try {
            String XADISK_SYSTEM_DIRECTORY = "C:\\XADiskSystem";
            String TEST_ROOT = "C:";
            /* if the specified XADiskSystemDirectory doesn't exist, it will be created automatically.
             * The contents of this directory should not be modified manually or any other software. In case you ever want to run
             * multiple applications on a single machine with XADisk, there are 2 requirements:
             * 1. Those multiple applications shouldn't work on common files/directories.
             * 2. Each one of them should use a different value for the XADiskSystemDirectory.
             */
            StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(XADISK_SYSTEM_DIRECTORY);
            xaf = NativeXAFileSystem.bootXAFileSystemStandAlone(configuration);
            xaf.waitForBootup(10000L);

            File testFile = new File(TEST_ROOT + "\\test.txt");

            Session session = xaf.createSessionForLocalTransaction();
            session.createFile(testFile, false);
            /* "false", because we want to create a normal file, not a directory.*/

            /* ...
             * ...
             * ...
             *
             * You can do more file operations here, by calling APIs for reading/writing/creating/deleting/updating files and directories.
             * Please refer to the XADisk User Guide, which mentions the kind of APIs for these file-system operations.
             * 
             * ...
             * ...
             * ...
             */

            session.commit();
            /*for standalone java applications, give "true", which indicates a one-phase (XA terminology) commit.*/

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (xaf != null) {
                try {
                    xaf.shutdown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

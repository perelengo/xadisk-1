/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xadisk.tests;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import org.xadisk.additional.XAFileInputStreamWrapper;
import org.xadisk.additional.XAFileOutputStreamWrapper;
import org.xadisk.bridge.proxies.interfaces.Session;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

/**
 *
 * @author nverma
 */
public class TestWrapperStreams {
    private static final String SEPERATOR = File.separator;
    private static final String currentWorkingDirectory = System.getProperty("user.dir");
    private static final String XADiskSystemDirectory = currentWorkingDirectory + SEPERATOR + "XADiskSystem";

    public static void main(String args[]) {
        try {
            StandaloneFileSystemConfiguration configuration = new StandaloneFileSystemConfiguration(XADiskSystemDirectory);
            configuration.setWorkManagerCorePoolSize(100);
            configuration.setWorkManagerMaxPoolSize(100);
            configuration.setServerPort(9998);

            NativeXAFileSystem xaFileSystem = NativeXAFileSystem.bootXAFileSystemStandAlone(configuration);
            xaFileSystem.waitForBootup(-1L);

            Session session = xaFileSystem.createSessionForLocalTransaction();
            InputStream is = new XAFileInputStreamWrapper(session.createXAFileInputStream(new File("a.txt"), false));
            is.mark(100);
            System.out.println((char)is.read());
            System.out.println((char)is.read());
            System.out.println((char)is.read());
            is.reset();
            System.out.println((char)is.read());
            is.close();

            OutputStream os = new XAFileOutputStreamWrapper(session.createXAFileOutputStream(new File("b.txt"), false));
            os.write('a');
            os.write('b');
            os.close();
            session.commit();

            System.out.println("XADisk System is up for use...");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

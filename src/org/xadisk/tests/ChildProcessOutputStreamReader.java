package org.xadisk.tests;

import java.io.InputStream;
import java.io.OutputStream;

class ChildProcessOutputStreamReader implements Runnable {

    private InputStream is;
    private OutputStream os;
    private static final int BUFFER_SIZE = 1000;
    private byte buffer[] = new byte[BUFFER_SIZE];

    public ChildProcessOutputStreamReader(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
    }
    
    public void run() {
        try {
            while (true) {
                int numRead = is.read(buffer);
                if (numRead == -1) {
                    return;
                }
                os.write(buffer, 0, numRead);
                os.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

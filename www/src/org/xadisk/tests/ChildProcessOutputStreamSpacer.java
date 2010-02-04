package org.xadisk.tests;

import java.io.InputStream;

class ChildProcessOutputStreamSpacer implements Runnable {

    private InputStream is;
    private static final int BUFFER_SIZE = 1000;
    private byte buffer[] = new byte[BUFFER_SIZE];

    public ChildProcessOutputStreamSpacer(InputStream is) {
        this.is = is;
    }

    @Override
    public void run() {
        try {
            int nowToRead = 1;
            while (true) {
                if (is.available() > BUFFER_SIZE) {
                    nowToRead = BUFFER_SIZE;
                } else if (is.available() == 0) {
                    nowToRead = 1;
                } else {
                    nowToRead = is.available();
                }
                int numRead = is.read(buffer, 0, nowToRead);
                if (numRead == -1) {
                    return;
                }
                System.out.print(new String(buffer, 0, numRead));
                System.out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

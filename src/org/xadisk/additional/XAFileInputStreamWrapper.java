package org.xadisk.additional;

import java.io.IOException;
import java.io.InputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileInputStream;

public class XAFileInputStreamWrapper extends InputStream {

    private XAFileInputStream xis;
    private long latestMarkPoint = -1;
    
    public XAFileInputStreamWrapper(XAFileInputStream xis) {
        this.xis = xis;
    }

    @Override
    public int available() throws IOException {
        try {
            return xis.available();
        } catch(Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        try {
            xis.close();
        } catch(Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public synchronized void mark(int readlimit) {
        this.latestMarkPoint = xis.position();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read() throws IOException {
        try {
            return xis.read();
        } catch(Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        try {
            return xis.read(b);
        } catch(Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            return xis.read(b, off, len);
        } catch(Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        //why do the mark/reset methods are syncronous in the IS clases.
        if(latestMarkPoint == -1) {
            throw new IOException("No corresponding mark does exist.");
        }
        try {
            //we do not honor the readlimit; a more flexible approach, which IS spec also allows.
            xis.position(latestMarkPoint);
            this.latestMarkPoint = -1;
        } catch(Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        try {
            return xis.skip(n);
        } catch(Exception e) {
            throw new IOException(e.getMessage());
        }
    }
}

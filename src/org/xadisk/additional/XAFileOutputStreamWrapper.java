package org.xadisk.additional;

import java.io.IOException;
import java.io.OutputStream;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.filesystem.exceptions.XAApplicationException;

public class XAFileOutputStreamWrapper extends OutputStream {

    private XAFileOutputStream xos;

    public XAFileOutputStreamWrapper(XAFileOutputStream xos) {
        this.xos = xos;
    }

    @Override
    public void close() throws IOException {
        try {
            xos.close();
        } catch (XAApplicationException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            xos.flush();
        } catch (XAApplicationException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        try {
            xos.write(b);
        } catch (XAApplicationException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void write(int b) throws IOException {
        try {
            xos.write(b);
        } catch (XAApplicationException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            xos.write(b, off, len);
        } catch (XAApplicationException e) {
            throw new IOException(e.getMessage());
        }
    }
}

package org.xadisk.filesystem.exceptions;

/**
 * This exception is thrown by the XA File I/O Streams when an operation
 * is invoked on the stream, but it could not be performed because the stream
 * is already closed.
 */
public class ClosedStreamException extends XAApplicationException {

    public ClosedStreamException() {
        super();
    }
    
}

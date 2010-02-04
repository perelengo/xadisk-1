package org.xadisk.tests;

public class AssertionFailedException extends Exception {

    public AssertionFailedException(String reason) {
        super(reason);
    }

    public AssertionFailedException(Throwable cause) {
        super(cause);
    }

}

/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.tests.correctness;

public class AssertionFailedException extends Exception {

    private static final long serialVersionUID = 1L;
    
    public AssertionFailedException(String reason) {
        super(reason);
    }

    public AssertionFailedException(Throwable cause) {
        super(cause);
    }

}

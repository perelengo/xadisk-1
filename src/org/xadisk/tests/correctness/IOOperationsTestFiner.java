/*
Copyright © 2010, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.tests.correctness;

import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ThreadStartRequest;
import java.util.ArrayList;
import java.util.List;

/*
public class IOOperationsTestFiner implements Runnable {

    private VirtualMachine vm;
    private EventQueue queue;
    private EventRequestManager erManager;
    private String transactionDemarcatingThread;
    private String className = org.xadisk.tests.CoreXAFileSystemTests.class.getName();
    private ReferenceType ref = null;
    private Method method = null;
    private ThreadReference tref = null;
    private List<Location> allLinesInMethod = null;
    private int nextLocationToBreakAt = -1;
    private final int startBreakPoint;
    private ReferenceType tuRef = null;

    public IOOperationsTestFiner(VirtualMachine vm, String transactionDemarcatingThread, int startBreakPoint) {
        this.vm = vm;
        this.queue = vm.eventQueue();
        this.erManager = vm.eventRequestManager();
        this.transactionDemarcatingThread = transactionDemarcatingThread;
        this.startBreakPoint = startBreakPoint;
    }

    public void run() {
        thisRunnable:
        while (true) {
            try {
                EventSet eventSet = queue.remove();
                EventIterator eventIter = eventSet.eventIterator();
                while (eventIter.hasNext()) {
                    Event event = eventIter.next();
                    if (event instanceof BreakpointEvent) {
                        checkpoint();
                        setBreakPointAtNextExistingLine();
                    } else if (event instanceof ClassPrepareEvent) {
                        ClassPrepareEvent cpEvent = (ClassPrepareEvent) event;
                        ref = cpEvent.referenceType();
                        method = ref.methodsByName("testIOOperations").get(0);
                        allLinesInMethod = method.allLineLocations();
                        setBreakPointAtNextExistingLine();
                    } else if (event instanceof ThreadStartEvent) {
                        ThreadStartEvent tsEvent = (ThreadStartEvent) event;
                        if (tsEvent.thread().name().equals(transactionDemarcatingThread)) {
                            tref = tsEvent.thread();
                            erManager.deleteEventRequest(tsEvent.request());
                        }
                    } else if (event instanceof VMStartEvent) {
                        ClassPrepareRequest cpRequest = erManager.createClassPrepareRequest();
                        cpRequest.addClassFilter(className);
                        cpRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
                        cpRequest.enable();
                        ThreadStartRequest tsRequest = erManager.createThreadStartRequest();
                        tsRequest.enable();
                        System.out.println("VM started.");
                    } else if (event instanceof VMDeathEvent) {
                        System.out.println("VM terminated.");
                        break thisRunnable;
                    } else if (event instanceof VMDisconnectEvent) {
                        System.out.println("VM got disconnected.");
                        break thisRunnable;
                    }
                }
                eventSet.resume();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void setBreakPointAtNextExistingLine() throws Exception {
        Location location = null;
        if (nextLocationToBreakAt == -1) {
            for (int i = 0; i < allLinesInMethod.size(); i++) {
                Location loc = allLinesInMethod.get(i);
                if (loc.lineNumber() >= startBreakPoint) {
                    nextLocationToBreakAt = i;
                    break;
                }
            }
        }
        if (nextLocationToBreakAt == allLinesInMethod.size()) {
            //all work over...return back to top and end this thread.
            return;
        }
        location = allLinesInMethod.get(nextLocationToBreakAt);
        System.out.println("Will set next breakpoint at line number : " + location+ " " + nextLocationToBreakAt);
        BreakpointRequest bpRequest = erManager.createBreakpointRequest(location);
        bpRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        bpRequest.enable();
        nextLocationToBreakAt++;
    }

    private void checkpoint() throws Exception {
        ClassObjectReference cor = (ClassObjectReference) ref.classObject();
        ObjectReference or;
        or = ref.instances(0).get(0);
        Method m = ref.methodsByName("checkpoint").get(0);
        List l = new ArrayList();
        or.invokeMethod(tref, m, l, 0);
    }
    private void commitCurrentSession() throws Exception {
        ObjectReference sessionReference = (ObjectReference) ref.getValue(ref.fieldByName("ioOperationsSession"));
        ReferenceType t = vm.classesByName(org.xadisk.bridge.proxies.interfaces.Session.class.getName()).get(0);
        Method m = t.methodsByName("commit").get(0);
        List l = new ArrayList();
        l.add(vm.mirrorOf(true));
        sessionReference.invokeMethod(tref, m, l, ObjectReference.INVOKE_SINGLE_THREADED);
    }

    private void resetCurrentSession() throws Exception {
        ClassObjectReference cor = (ClassObjectReference) ref.classObject();
        ObjectReference or;
        or = ref.instances(0).get(0);
        Method m = ref.methodsByName("resetIOOperationsSession").get(0);
        List l = new ArrayList();
        or.invokeMethod(tref, m, l, 0);
        //the following approach could not be applied...strangely, invoking a static method
        //on a ClassObjectReference was giving an error "Invalid Method". Though the method object
        //was correctly being return from ref.methodsByName. So, we used a hack by instanting
        //an object of the target class and call the restIO*Session on that object instead of ClassObjectReference.
        //cor.invokeMethod(tref, m, l, 0);
    }

    private void verifyTransactionView() throws Exception {
        if(tuRef == null) {
            tuRef = vm.classesByName(org.xadisk.tests.TestUtility.class.getName()).get(0);
        }
        ObjectReference or;
        or = tuRef.instances(0).get(0);
        Method m = tuRef.methodsByName("compareDiskAndView").get(0);
        List l = new ArrayList();
        l.add(tref.frame(0).getValue(method.variablesByName("fileRoot2").get(0)));
        l.add(tref.frame(0).getValue(method.variablesByName("fileRoot1").get(0)));
        //l.add(tref.getValue(ref.fieldByName("ioOperationsSession")));
        l.add(ref.instances(0).get(0).getValue(ref.fieldByName("ioOperationsSession")));
        or.invokeMethod(tref, m, l, 0);
    }

    private void verifyTransactionAfterCommit() throws Exception {
        if(tuRef == null) {
            tuRef = vm.classesByName(org.xadisk.tests.TestUtility.class.getName()).get(0);
        }
        ObjectReference or;
        or = tuRef.instances(0).get(0);
        Method m = tuRef.methodsByName("compareDiskAndDisk").get(0);
        List l = new ArrayList();
        l.add(tref.frame(0).getValue(method.variablesByName("fileRoot2").get(0)));
        l.add(tref.frame(0).getValue(method.variablesByName("fileRoot1").get(0)));
        or.invokeMethod(tref, m, l, 0);
    }
}
*/
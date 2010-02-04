package org.xadisk.tests;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.Connector.StringArgument;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Temporary {

    public static void main(String args[]) {
        try {
            if (args.length > 0 && args[0].equals("noop")) {
                toBreakAt();
                return;
            }
            LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
            Map connectorArguments = connector.defaultArguments();
            Iterator iter = connectorArguments.values().iterator();
            while (iter.hasNext()) {
                Argument arg = (Argument) iter.next();
                System.out.println(arg.name() + " of type " + arg.getClass() + "______" + arg.description());
                System.out.println("\t\t ...default value ; "+arg.value());
            }
            StringArgument mainArgument = (StringArgument) connectorArguments.get("main");
            mainArgument.setValue("org.xadisk.tests.TestAllDimensions forRunningTests");
            VirtualMachine vm = connector.launch(connectorArguments);
            EventRequestManager erMgr = vm.eventRequestManager();
            List<ReferenceType> allClasses = vm.allClasses();
            for (int i = 0; i < allClasses.size(); i++) {
                ReferenceType refType = allClasses.get(i);
                if (refType.name().equals(org.xadisk.filesystem.GatheringDiskWriter.class.getName())) {
                    Method method = refType.methodsByName("submitBuffer").get(0);
                    Location location = method.location();
                    BreakpointRequest bpRequest = erMgr.createBreakpointRequest(location);
                    bpRequest.enable();
                }
            }
            vm.process().waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void toBreakAt() {
        System.out.println("Reached.");
    }
}

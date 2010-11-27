package org.xadisk.bridge.proxies.facilitators;

import java.io.Serializable;

public class SerializedMethod implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private final String className;
    private final String methodName;
    private final String[] parameterTypesNames;

    public SerializedMethod(String className, String methodName, String[] parameterTypesNames) {
        this.className = className;
        this.methodName = methodName;
        this.parameterTypesNames = parameterTypesNames;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String[] getParameterTypesNames() {
        return parameterTypesNames;
    }
}

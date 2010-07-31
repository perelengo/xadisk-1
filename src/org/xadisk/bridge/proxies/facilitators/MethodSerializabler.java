package org.xadisk.bridge.proxies.facilitators;

import java.io.IOException;
import java.lang.reflect.Method;

public class MethodSerializabler implements Serializabler<Method, SerializedMethod> {

    public MethodSerializabler() {
    }

    public SerializedMethod serialize(Method method) {
        String methodName = method.getName();
        String className = method.getDeclaringClass().getName();
        Class[] parameterTypes = method.getParameterTypes();
        String[] parameterTypesNames = new String[parameterTypes.length];
        for (int i = 0; i < parameterTypesNames.length; i++) {
            parameterTypesNames[i] = parameterTypes[i].getName();
        }
        return new SerializedMethod(className, methodName, parameterTypesNames);
    }

    public Method reconstruct(SerializedMethod serializedMethod) throws IOException {
        try {
            Class clazz = Class.forName(serializedMethod.getClassName());
            String[] parameterTypesNames = serializedMethod.getParameterTypesNames();
            Class parameterTypes[] = new Class[parameterTypesNames.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                parameterTypes[i] = Class.forName(parameterTypesNames[i]);
            }
            return clazz.getMethod(serializedMethod.getMethodName(), parameterTypes);
        } catch (Throwable t) {
            IOException ioException = new IOException("A Method object could not be constructed from the object stream.");
            ioException.initCause(t);
            throw ioException;
        }
    }
}

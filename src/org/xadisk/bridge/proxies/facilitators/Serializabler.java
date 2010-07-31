package org.xadisk.bridge.proxies.facilitators;

import java.io.IOException;
import java.io.Serializable;

public interface Serializabler<T1, T2 extends Serializable> {

    public T1 reconstruct(T2 t2) throws IOException;

    public T2 serialize(T1 t1);
}

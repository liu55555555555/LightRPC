package com.Lrpc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


public class ServiceConfig<T> {
    private Class<?> interfaceClass;
    private Object ref;

    public ServiceConfig() {
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public void setRef(Object ref) {
        this.ref = ref;
    }

    public Object getRef() {
        return ref;
    }
}

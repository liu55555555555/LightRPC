package com.Lrpc.impl;

import com.Lrpc.sayHello;

public class sayHelloImpl implements sayHello {
    @Override
    public String HelloRPC(String msg) {
        return "hello consumer:"+msg;
    }
}

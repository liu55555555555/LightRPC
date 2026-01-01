package com.Lrpc.impl;

import com.Lrpc.annotation.Api;
import com.Lrpc.sayHello;

@Api
public class sayHelloImpl implements sayHello {
    @Override
    public String HelloRPC(String msg) {
        return msg;
    }
}

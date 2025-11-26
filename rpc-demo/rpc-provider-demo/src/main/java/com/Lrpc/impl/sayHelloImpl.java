package com.Lrpc.impl;

import com.Lrpc.sayHello;

public class sayHelloImpl implements sayHello {
    @Override
    public String HelloRPC(String msg) {
        return "这里是我提供给你的服务，你给我的参数："+msg;
    }
}

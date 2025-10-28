package com.Lrpc;

public class sayHelloImpl implements sayHello{
    @Override
    public String HelloRPC(String msg) {
        return "hello consumer:"+msg;
    }
}

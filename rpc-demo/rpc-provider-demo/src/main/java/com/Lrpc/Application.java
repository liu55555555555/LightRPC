package com.Lrpc;

import com.Lrpc.impl.sayHelloImpl;

public class Application {
    //这里的代码，只要一运行main方法，这里的代码就会先执行，去启动服务
    public static void main(String[] args) {
       //服务提供方需要注册服务，启动服务
        //1.封装要发布的服务
        ServiceConfig<sayHello> service = new ServiceConfig<>();
        service.setInterfaceClass(sayHello.class);
        service.setRef(new sayHelloImpl());
        //2.定义注册中心


        //3.通过启动引导程序，启动服务提供方
        // (1.)配置 --应用的名称 --注册中心 -- 序列化协议 --压缩方式
        //（2.）发布
        //（3.）启动
        LrpcBootstrap.getInstance()
                .application("first-provider-application")
                //配置注册中心
                .register(new RegistryConfig("zookeeper://127.0.0.1:2181"))//集群的话逗号跟上就行
                .protocol(new ProtocolConfig("jdk"))
                //发布服务
                .publish(service)
                //启动服务
                .start();
        //这里的链式调用相当于下面的代码
//        LrpcBootstrap bootstrap = LrpcBootstrap.getInstance();
//        bootstrap = bootstrap.application("first-provider-application");
//        bootstrap = bootstrap.register(new RegistryConfig("zookeeper://127.0.0.1:2181"));
//        bootstrap = bootstrap.protocol(new ProtocolConfig("jdk"));
//        bootstrap = bootstrap.publish(service);
//        bootstrap.start();

    }
}

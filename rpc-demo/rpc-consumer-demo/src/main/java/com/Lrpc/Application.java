package com.Lrpc;

public class Application {
    public static void main(String[] args) {
        //获取RPC调用代理对象，使用ReferenceConfig进行封装(注意：这里的代理对象并不是获取咱们的服务类的代理对象，其实是一个携带了一些链接信息，参数信息的消费者代理类对象，这个代理类对象去通过网络去调用目标方法)
        ReferenceConfig<sayHello> reference=new ReferenceConfig<>();
        reference.setInterfaceClass(sayHello.class);

        //代理做什么：
        //1.链接注册中心
        //2.拉取服务列表
        //3.选择一个服务并建立连接
        //4.发送请求，携带一些信息（接口名，参数列表，方法的名字），获得结果
        LrpcBootstrap.getInstance()
                .application("first-consumer")
                .register(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .reference(reference);

        //获取一个代理对象
        sayHello hello=reference.get();
        System.out.println(hello.HelloRPC("你好"));

    }

}

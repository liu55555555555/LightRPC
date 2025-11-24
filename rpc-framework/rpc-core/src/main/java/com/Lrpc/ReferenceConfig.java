package com.Lrpc;

import com.Lrpc.Exception.NetworkException;
import com.Lrpc.discovery.Registry;

import com.Lrpc.proxy.handler.RpcConsumerInvocationHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import io.netty.channel.ChannelFutureListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class ReferenceConfig<T> {
    private Class<T> interfaceClass;

    //需要一个注册中心来发现服务
    private Registry registry;

    /**
     * 获取代理对象，这个代理对象的作用是将参数啥的通过netty传给服务端
     * @return
     */
    public T get() {
        //此处使用动态代理实现代理对象
        //ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = sayHello.class.getClassLoader();//类加载器，可以用要代理的接口的类加载器/也可用当前线程的类加载器,如上行代码所示（还可以解决当要代理的接口不止一个时要怎么选择类加载器的问题）
        Class[] classes = new Class[]{interfaceClass};//一个装有要代理的类的class数组
        InvocationHandler invocationHandler = new RpcConsumerInvocationHandler(registry, interfaceClass);

        //使用java原生态的动态代理生成代理对象
        Object ret = Proxy.newProxyInstance(classLoader, classes, invocationHandler);

        return (T) ret;
    }


    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }
}

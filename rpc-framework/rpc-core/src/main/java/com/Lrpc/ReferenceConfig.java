package com.Lrpc;

import com.Lrpc.Exception.NetworkException;
import com.Lrpc.discovery.Registry;

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

        Object ret = Proxy.newProxyInstance(classLoader,
                classes,
                (proxy, method, args) -> {
                    log.info("method--->{}", method);
                    log.info("args--->{}", args);
                    //通过method获取方法名，通过args获取参数列表
                    //1.发现服务，从注册中心寻找一个可用的服务（实际获取的就是可用服务的地址）
                    InetSocketAddress serverAddress = registry.lookup(interfaceClass.getName());//调用方法返回一个InetSocketAddress对象，其中将ip地址和端口号封装在InetSocketAddress对象里了
                    if(log.isDebugEnabled()){
                        log.debug("从注册中心发现服务：{}的可用主机：[{}]",interfaceClass.getName(),serverAddress);
                    }


                    //2.用netty链接服务器，发送 调用的服务的 名字、方法名字、参数列表，得到结果
                    //todo: q:整个链接过程放在这里行不行，也就意味着每次调用都会产生一个新的netty链接。   解决：缓存我们的channel链接，先尝试从缓存中获取channel，如果没有，在创建新的连接，并进行缓存。
                    //        不正确的代码：NioEventLoopGroup group = new NioEventLoopGroup();
                    //        也就是说每次在此处建立一个新的连接是不合适的

                    //1).尝试从全局缓存中获取一个channel
                    Channel channel = LrpcBootstrap.CHANNEL_CACHE.get(serverAddress);
                    if(channel == null) {

                        /**
                         * 如果从全局中获取不到channel则创建一个新的channel，并存入缓存-------同步策略----------------
                         */
//                        channel = NettyBootStrapInitializer.getBootstrap()
//                                .connect(serverAddress)
//                                .await().channel();
                        //await 方法会阻塞，会等待链接成功后再返回，netty还提供了异步处理的逻辑
                        //sync和await都是阻塞当前线程，获取返回值（因为链接的过程是异步的，发送数据的过程（即writeAndFlush方法就是发送数据的过程）是异步的-->netty本身是异步的，await和sync都可以通过阻塞实现同步）
                        //如果发生了异常，sync会主动在主线程抛出异常，await不会，异常会在子线程中处理（异常存储在 future.cause() 中），需要在future中处理（主线程需要手动检查 future.isSuccess()）

                        /**
                         * 如果从全局中获取不到channel则创建一个新的channel，并存入缓存-------异步策略----------------
                         * todo: q: 我们创建了addListener异步链接，但是get方法还是会阻塞，这段代码其实与上面的同步的方法差不多，但是更加灵活了：future.get(3, TimeUnit.SECONDS);这里可以替换成不阻塞的方法。
                         */
                        //使用addListener执行的异步操作
                        CompletableFuture<Channel> future = new CompletableFuture<>();//CompletableFuture：是 Java 8 引入的异步编程工具，用于处理异步计算的结果。
                        NettyBootStrapInitializer.getBootstrap()
                                .connect(serverAddress)
                                .addListener(//addListener就是告诉 Netty："连接完成后，请调用我里面的这个(ChannelFutureListener)promise -> {。。。。函数"，其中是netty的I/O线程进行的回调
                                (ChannelFutureListener)promise -> {
                                    // 这个函数会在连接完成时被 Netty 的 I/O 线程调用
                                    if(promise.isDone()){
                                        // 连接完成了，把结果（promise.channel()）放到我们之前创建的"盒子（CompletableFuture<Channel> future）"里
                                        future.complete(promise.channel());
                                    }else if(!promise.isSuccess()){
                                        // 连接失败了，把异常放到"盒子（CompletableFuture<Channel> future）"里
                                        future.completeExceptionally(promise.cause());
                                    }
                                });
                        //从盒子（CompletableFuture<Channel> future）中获取channel
                        channel = future.get(3, TimeUnit.SECONDS);
                        //存入缓存
                        LrpcBootstrap.CHANNEL_CACHE.put(serverAddress,channel);
                    }
                    if(channel ==  null){
                        log.debug("获取通道时[{}]发生了异常",serverAddress);
                        throw new NetworkException("获取通道时发生了异常");
                    }

                    /**
                     * 同步策略，但是一直出现阻塞本身不是什么好事，我们要使用异步策略-------------------------------------------
                     */
//                        ChannelFuture channelFuture = channel.writeAndFlush(new Object()).await();
//                        if(!channelFuture.isSuccess()){
//                            //需捕获异常，可以捕获异步任务中的异常
//                            Throwable cause = channelFuture.cause();
//                            throw new NetworkException("发送数据时发生了异常");
//                        }else if(channelFuture.isDone()){
//                            Object result = channelFuture.getNow();
//                        }

                    /**
                     * 异步策略---------------------------------------------------------------------------------------
                     */
                    CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                    //todo ：将completableFuture暴露出去
                    LrpcBootstrap.PENDING_REQUESTS.put(1L,completableFuture);

                    channel.writeAndFlush(Unpooled.copiedBuffer("Hello server,I am client".getBytes()))
                            .addListener((ChannelFutureListener) promise -> {
                        //当前的promise将来返回的结果是writeAndFlush的返回结果
                        //但是writeAndFlush一旦将数据写出去，这个writeAndFlush就结束关闭了，没有返回值
                        //所以promise也就关闭了，而我们要的是服务端给我们的返回值，所以这里这个if判断isDone是有问题的，最后promise.getNow()获取是个null
                        //所以我们要将 completableFuture 挂起并且暴露，让服务器可以去使用这个completableFuture，这样我们就可以得到服务提供方给我们的放回值了，得到服务提供放的响应后在调用complete方法
                        //todo ：将completableFuture暴露出去
//                        if(promise.isDone()){
//                            completableFuture.complete(promise.getNow());
//                        } else

                        //我们只需处理一下异常就可以了
                        if (!promise.isSuccess()) {
                            completableFuture.completeExceptionally(promise.cause());
                        }
                    });
                    //如果没有处理这个 completableFuture ， 这里会阻塞，等待complete方法的执行
                    //q:我们需要再哪里调用complete方法得到结果，很明显 pipeline 中最终的handler的处理结果
                    return completableFuture.get(10, TimeUnit.SECONDS);
                });
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

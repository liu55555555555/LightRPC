package com.Lrpc;

import com.Lrpc.discovery.Registry;
import com.Lrpc.discovery.RegistryConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class LrpcBootstrap {

    private String appName;
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;
    //注册中心
    private Registry register;
    //端口号
    private int port=8088;
    // netty链接的缓存，如果使用InetSocketAddress这样的“类”做key，一定要看他有没有重写equals方法和toString方法
    public final static Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);

    //全局的服务列表：维护已经发布的服务列表 key -> interface的全限定名  value -> ServiceConfig<?>
    public static final Map<String,ServiceConfig<?>> SERVERS_LIST = new ConcurrentHashMap<>(16);

    //定义全局的对外挂起的 completableFuture
    public static final Map<Long,CompletableFuture<Object>> PENDING_REQUESTS = new ConcurrentHashMap<>(128);

    //构造器私有化
    private LrpcBootstrap(){
        //构造启动引导程序时需要做一些什么初始化的事
    }

    //LrpcBootstrap是个单例，我们希望每个应用程序只有一个实例
    private static LrpcBootstrap lrpcBootstrap=new LrpcBootstrap();
    public static LrpcBootstrap getInstance() {
        return lrpcBootstrap;
    }

    /**
     * 用来定义当前应用的名字
     * @param appName
     * @return this当前实例
     */
    public LrpcBootstrap application(String appName) {
        this.appName = appName;
        return this;
    }

    /**
     * 配置一个注册中心
     * @param registryConfig 注册中心
     * @return this当前实例
     */
    public LrpcBootstrap register(RegistryConfig registryConfig) {
        //我们其实更加希望以后可以扩展更多中不同的实现
        //尝试用 registryConfig 获取一个注册中心，有点工厂设计模式的意思了
        this.register = registryConfig.getRegistry();
        return this;
    }


    /**
     * 配置当前暴露的服务使用的协议
     * @param protocolConfig
     * @return this当前实例
     */
    public LrpcBootstrap protocol(ProtocolConfig protocolConfig) {
        this.protocolConfig = protocolConfig;
        if(log.isDebugEnabled()){
            log.debug("当前工程使用了：{}协议进行序列化",protocolConfig.getProtocolName());
        }
        return this;
    }


    /**
     * =======================================服务提供方的相关api============================================
     */

    /**
     * 发布服务，将接口实现，注册到服务中心
     * @param service 封装的需要发布的服务
     * @return
     */
    public LrpcBootstrap publish(ServiceConfig<?> service) {
        register.register(service);
        SERVERS_LIST.put(service.getInterfaceClass().getName(),service);
        if(log.isDebugEnabled()){
            log.debug("服务：{}已被注册",service.getInterfaceClass().getName());
        }
        return this;
    }

    /**
     * 批量发布服务
     * @param services 封装的需要发布的服务集合
     * @return
     */
    public LrpcBootstrap publish(List<ServiceConfig<?>> services) {
        for(ServiceConfig<?> service:services){
            publish(service);
        }
        return this;
    }

    /**
     * 服务器端创建netty，并启动netty服务
     */
    @SneakyThrows
    public void start() {


        // 1、创建eventLoop，老板只负责处理请求，之后会将请求分发至worker
        EventLoopGroup boss = new NioEventLoopGroup(2);
        EventLoopGroup worker = new NioEventLoopGroup(10);
        try {

            // 2、需要一个服务器引导程序
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 3、配置服务器
            serverBootstrap = serverBootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        //这里是核心，我们需要添加很多入站和出站的channelHandler
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new SimpleChannelInboundHandler<>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
                                    ByteBuf byteBuf = (ByteBuf) msg;
                                    log.info("服务端收到客户端传来的信息[{}]",byteBuf.toString(Charset.defaultCharset()));

                                    //可以写数据返回给客户端，也可以不写回去
                                    //channelHandlerContext.channel().writeAndFlush("服务器已经收你的信息");//在Handler内部优先使用 ctx.writeAndFlush()
                                    channelHandlerContext.channel().writeAndFlush(Unpooled.copiedBuffer("服务端已经接收到消息，现在给你返回---->server".getBytes(Charset.defaultCharset())));

                                }
                            });
                        }
                    });

            // 4、绑定端口（监听客户端发来的连接请求）
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();

            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e){
            e.printStackTrace();
        } finally {
            try {
                boss.shutdownGracefully().sync();
                worker.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }    }



    /**
     * =======================================调用方的相关api============================================
     */
    public LrpcBootstrap reference(ReferenceConfig<?> reference) {
        reference.setRegistry(register);
        if(log.isDebugEnabled()){
            log.debug("服务：{}已被引用",reference.getInterfaceClass().getName());
        }

        //在这个方法里我们是否可以拿到相关的配置项-注册中心
        //配置reference，将来调用get方法时，方便生成代理对象
        return this;

    }


}

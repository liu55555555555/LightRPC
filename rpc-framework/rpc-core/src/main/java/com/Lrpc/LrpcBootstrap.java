package com.Lrpc;

import com.Lrpc.channelhandler.handler.LrpcRequestDecoder;
import com.Lrpc.channelhandler.handler.LrpcResponseEncoder;
import com.Lrpc.channelhandler.handler.MethodCallHandler;
import com.Lrpc.core.HeartbeatDetector;
import com.Lrpc.discovery.Registry;
import com.Lrpc.discovery.RegistryConfig;
import com.Lrpc.loadbalancer.ConsistentHashBalancer;
import com.Lrpc.loadbalancer.LoadBalancer;
import com.Lrpc.loadbalancer.MinTimeBalancer;
import com.Lrpc.transport.message.LrpcRequest;
import com.Lrpc.utils.IdGenerator;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class LrpcBootstrap {


    public static final int PORT = 8090;
    private String appName;
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;
    //注册中心
    private Registry register;
    public static LoadBalancer LOAD_BALANCER;
    // netty链接的缓存，如果使用InetSocketAddress这样的“类”做key，一定要看他有没有重写equals方法和toString方法
    public final static Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);
    public static final TreeMap<Long,Channel> ANSWER_TIME_CHANNEL_CACHE = new TreeMap<>();

    //全局的服务列表：维护已经发布的服务列表 key -> interface的全限定名  value -> ServiceConfig<?>
    public static final Map<String,ServiceConfig<?>> SERVERS_LIST = new ConcurrentHashMap<>(16);


    //定义全局的对外挂起的 completableFuture
    public static final Map<Long,CompletableFuture<Object>> PENDING_REQUESTS = new ConcurrentHashMap<>(128);

    //全局的id生成器
    public static final IdGenerator ID_GENERATOR = new IdGenerator(1,2);

    // 默认序列化方式为jdk
    public static String SERIALIZE_TYPE = "jdk";

    // 默认压缩方式为gzip
    public static String COMPRESS_TYPE = "gzip";

    // 线程本地变量
    public static final ThreadLocal<LrpcRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();



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
        // todo 修改
        LOAD_BALANCER = new MinTimeBalancer();
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
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new LoggingHandler())
                                    .addLast(new LrpcRequestDecoder())
                                    .addLast(new MethodCallHandler())
                                    .addLast(new LrpcResponseEncoder());

                        }
                    });

            // 4、绑定端口（监听客户端发来的连接请求）
            ChannelFuture channelFuture = serverBootstrap.bind(PORT).sync();

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
        }
    }



    /**
     * =======================================调用方的相关api============================================
     */
    public LrpcBootstrap reference(ReferenceConfig<?> reference) {

        // 开启对这个服务的心跳检测
        HeartbeatDetector.detectHeartbeat(reference.getInterfaceClass().getName());

        reference.setRegistry(register);
        if(log.isDebugEnabled()){
            log.debug("服务：{}已被引用",reference.getInterfaceClass().getName());
        }

        //在这个方法里我们是否可以拿到相关的配置项-注册中心
        //配置reference，将来调用get方法时，方便生成代理对象
        return this;

    }


    public LrpcBootstrap serialize(String serializeType) {
        SERIALIZE_TYPE = serializeType;
        if(log.isDebugEnabled()){
            log.debug("当前工程使用了：{}协议进行序列化。",serializeType);
        }
        return this;
    }

    public LrpcBootstrap compress(String compressType) {
        COMPRESS_TYPE = compressType;
        if(log.isDebugEnabled()){
            log.debug("当前工程使用了：{}压缩算法进行压缩。",compressType);
        }
        return this;
    }

    public Registry getRegister() {
        return register;
    }
}

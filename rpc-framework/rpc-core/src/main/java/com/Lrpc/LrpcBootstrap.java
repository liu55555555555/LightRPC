package com.Lrpc;

import com.Lrpc.annotation.Api;
import com.Lrpc.channelhandler.handler.LrpcRequestDecoder;
import com.Lrpc.channelhandler.handler.LrpcResponseEncoder;
import com.Lrpc.channelhandler.handler.MethodCallHandler;
import com.Lrpc.config.Configuration;
import com.Lrpc.core.HeartbeatDetector;
import com.Lrpc.discovery.Registry;
import com.Lrpc.discovery.RegistryConfig;
import com.Lrpc.loadbalancer.*;
import com.Lrpc.transport.message.LrpcRequest;
import com.Lrpc.utils.IdGenerator;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class LrpcBootstrap {


    // 全局的配置中心
    private Configuration configuration;


    // netty链接的缓存，如果使用InetSocketAddress这样的“类”做key，一定要看他有没有重写equals方法和toString方法
    public final static Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);
    public static final TreeMap<Long,Channel> ANSWER_TIME_CHANNEL_CACHE = new TreeMap<>();

    //全局的服务列表：维护已经发布的服务列表 key -> interface的全限定名  value -> ServiceConfig<?>
    public static final Map<String,ServiceConfig<?>> SERVERS_LIST = new ConcurrentHashMap<>(16);


    //定义全局的对外挂起的 completableFuture
    public static final Map<Long,CompletableFuture<Object>> PENDING_REQUESTS = new ConcurrentHashMap<>(128);

    // 线程本地变量,保存request对象，可以到当前线程中随时获取
    public static final ThreadLocal<LrpcRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();



    //构造器私有化
    private LrpcBootstrap(){
        //构造启动引导程序时需要做一些什么初始化的事
        configuration = new Configuration();
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
        configuration.setAppName(appName);
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
        configuration.setRegistryConfig(registryConfig);
        // todo 修改
        return this;
    }


    /**
     * 配置负载均衡策略
     * @param loadBalancer 注册中心
     * @return this当前实例
     */
    public LrpcBootstrap loadBalance(LoadBalancer loadBalancer) {
        //我们其实更加希望以后可以扩展更多中不同的实现
        //尝试用 registryConfig 获取一个注册中心，有点工厂设计模式的意思了
        configuration.setLoadBalancer(loadBalancer);
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
        configuration.getRegistryConfig().getRegistry().register(service);
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
            ChannelFuture channelFuture = serverBootstrap.bind(configuration.getPort()).sync();

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

        reference.setRegistry(configuration.getRegistryConfig().getRegistry());
        if(log.isDebugEnabled()){
            log.debug("服务：{}已被引用",reference.getInterfaceClass().getName());
        }

        //在这个方法里我们是否可以拿到相关的配置项-注册中心
        //配置reference，将来调用get方法时，方便生成代理对象
        return this;

    }


    public LrpcBootstrap serialize(String serializeType) {
        configuration.setSerializeType(serializeType);
        if(log.isDebugEnabled()){
            log.debug("当前工程使用了：{}协议进行序列化。",serializeType);
        }
        return this;
    }

    public LrpcBootstrap compress(String compressType) {
        configuration.setCompressType(compressType);
        if(log.isDebugEnabled()){
            log.debug("当前工程使用了：{}压缩算法进行压缩。",compressType);
        }
        return this;
    }




    public LrpcBootstrap scan(String packageName){


        // 1、通过packageName获取其下的所有类的权限定名称(包名.类名)
        List<String> classNames = getAllClassNames(packageName);

        // 2、通过反射获取他的接口，构建具体实现
        List<Class<?>> classes = classNames.stream()
                .map(className -> {
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).filter(clazz -> clazz.getAnnotation(Api.class) != null)
                .collect(Collectors.toList());

        for(Class<?> clazz : classes){
            Class<?>[] interfaces = clazz.getInterfaces();
            Object instance = null;
            try {
                instance = clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            for(Class<?> anInterface : interfaces){
                ServiceConfig<?> serviceConfig = new ServiceConfig<>();
                serviceConfig.setInterfaceClass(anInterface);
                serviceConfig.setRef(instance);

                if (log.isDebugEnabled()){
                    log.debug("服务：[{}]已被发布",anInterface.getName());
                }


                // 3、发布
                publish(serviceConfig);
            }

        }


        return  this;
    }

    private List<String> getAllClassNames(String packageName) {
        // 1.通过packageName获取绝对路径
        // com.Lrpc.xxx.yyy（包路径）------> E://xxx/xww/sss/com/lrpc/xxx/yyy
        String basePath = packageName.replaceAll("\\.", "/");// 这里是正则表达式，\\.在java的字面量中转义成 \.然后这个\.在正则表达式中转义成正常的.符号本身的意思。
        URL url = ClassLoader.getSystemClassLoader().getResource(basePath);
        if(url == null){
            throw new RuntimeException("包扫描时，未找到该包路径");
        }
        String absolutePath = url.getPath();
        List<String> classNames = new ArrayList<>();
        classNames = recursionFile(absolutePath, classNames, basePath);

        return classNames;

    }

    private List<String> recursionFile(String absolutePath, List<String> classNames,String basePath) {
        // 获取文件
        File file = new File(absolutePath);// 根据文件的路径创建一个文件对象，后续用这个对象的方法可以获取文件名等信息

        // 判断文件是否是文件夹
        if(file.isDirectory()){
            // 找到文件夹里的所有是文件夹的文件和以.class结尾的文件
            File[] child = file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory() || pathname.getName().endsWith(".class");
                }
            });
            for(File f:child){
                if (f.isDirectory()){
                    // 递归调用
                    recursionFile(f.getAbsolutePath(),classNames,basePath);
                }else{
                    // 文件-->类的权限定名称
                    String className = getClassNameByAbsolutePath(f.getAbsolutePath(),basePath);
                    System.out.println(className);
                    classNames.add(className);
                }
            }
        }else{
            // 文件-->类的权限定名称
            String className = getClassNameByAbsolutePath(absolutePath,basePath);
            System.out.println(className);
            classNames.add(className);
        }

        return classNames;

    }

    private String getClassNameByAbsolutePath(String absolutePath,String basePath) {
        // D:\Project\LightRPC\rpc-framework\rpc-core\target\classes\com\lrpc\transport\message\LrpcRequest.class------>
        // com\lrpc\transport\message\LrpcRequest.class------> com.lrpc.transport.message.LrpcRequest
        String fileName = absolutePath.substring(absolutePath.indexOf(basePath.replaceAll("/", "\\\\")))
                .replaceAll("\\\\", ".");

        fileName = fileName.substring(0,fileName.indexOf(".class"));
        return fileName;
    }

    public static void main(String[] args) {
        LrpcBootstrap.getInstance().getAllClassNames("com.Lrpc");
    }


    public Configuration getConfiguration() {
        return configuration;
    }
}

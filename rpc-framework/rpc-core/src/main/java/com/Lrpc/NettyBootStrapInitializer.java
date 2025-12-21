package com.Lrpc;

import com.Lrpc.channelhandler.ConsumerChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;


/**
 * todo:q
 * 提供netty单例
 */
@Slf4j
public class NettyBootStrapInitializer {

    private static final Bootstrap bootstrap = new Bootstrap();

    //放到静态代码块中，防止多线程访问时，创建多个实例，多次进行配置
    static {
        // 定义线程池，EventLoopGroup
        NioEventLoopGroup group = new NioEventLoopGroup();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ConsumerChannelInitializer());
    }

    //构造器私有化
    private NettyBootStrapInitializer() {}

    public static Bootstrap getBootstrap() {
        return bootstrap;
    }

}

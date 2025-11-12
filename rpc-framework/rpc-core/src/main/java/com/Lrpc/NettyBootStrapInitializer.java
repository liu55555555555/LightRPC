package com.Lrpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;


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
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf msg) throws Exception {
                                //服务提供方给与的结果
                                String ret = msg.toString(Charset.defaultCharset());
                                // 从全局挂起的请求中寻找与之匹配的待处理的 completableFuture
                                CompletableFuture<Object> future = LrpcBootstrap.PENDING_REQUESTS.get(1L);
                                future.complete(ret);
                            }
                        });//添加自己的channelHander
                    }
                });
    }

    //构造器私有化
    private NettyBootStrapInitializer() {}

    public static Bootstrap getBootstrap() {
        return bootstrap;
    }

}

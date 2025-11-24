package com.Lrpc.ChannelHandler.handler;

import com.Lrpc.LrpcBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

/**
 * 封装consumer的响应
 */
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf msg) throws Exception {
        //服务提供方给与的结果
        String ret = msg.toString(Charset.defaultCharset());
        // 从全局挂起的请求中寻找与之匹配的待处理的 completableFuture
        CompletableFuture<Object> future = LrpcBootstrap.PENDING_REQUESTS.get(1L);
        future.complete(ret);
    }
}

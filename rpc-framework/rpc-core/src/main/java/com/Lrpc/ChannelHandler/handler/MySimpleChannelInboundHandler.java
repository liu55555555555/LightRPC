package com.Lrpc.ChannelHandler.handler;

import com.Lrpc.LrpcBootstrap;
import com.Lrpc.transport.message.LrpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

/**
 * 封装consumer的响应
 */
@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<LrpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, LrpcResponse lrpcResponse) throws Exception {
        //服务提供方给与的结果
        Object returnValue = lrpcResponse.getResponseBody();
        // 从全局挂起的请求中寻找与之匹配的待处理的 completableFuture
        CompletableFuture<Object> future = LrpcBootstrap.PENDING_REQUESTS.get(1L);
        future.complete(returnValue);
        if(log.isDebugEnabled()){
            log.debug("已经寻找到编号为【{}】的CompletableFuture，响应结果为：【{}】",lrpcResponse.getRequestId(),returnValue.toString());
        }
    }
}

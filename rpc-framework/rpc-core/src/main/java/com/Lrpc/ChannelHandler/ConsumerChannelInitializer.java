package com.Lrpc.ChannelHandler;

import com.Lrpc.ChannelHandler.handler.LrpcRequestEncoder;
import com.Lrpc.ChannelHandler.handler.LrpcResponseDecoder;
import com.Lrpc.ChannelHandler.handler.MySimpleChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * 封装
 */
public class ConsumerChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline()
                // netty自带的日志处理器（记录所有pipeline事件和数据的日志处理器）
                .addLast(new LoggingHandler(LogLevel.DEBUG))
                // 消息编码器
                .addLast(new LrpcRequestEncoder())
                // 入站的消息解码器
                .addLast(new LrpcResponseDecoder())
                // 处理结果
                .addLast(new MySimpleChannelInboundHandler());
    }
}

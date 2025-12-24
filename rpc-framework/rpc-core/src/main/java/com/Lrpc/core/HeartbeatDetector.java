package com.Lrpc.core;

import com.Lrpc.LrpcBootstrap;
import com.Lrpc.NettyBootStrapInitializer;
import com.Lrpc.compress.CompressFactory;
import com.Lrpc.discovery.Registry;
import com.Lrpc.enumeration.RequestType;
import com.Lrpc.serialize.SerializerFactory;
import com.Lrpc.transport.message.LrpcRequest;
import com.Lrpc.transport.message.RequestPayload;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class HeartbeatDetector {

    public static void detectHeartbeat(String serviceName){
        // 1.注册中心拉取服务列表并建立连接
        Registry register = LrpcBootstrap.getInstance().getRegister();
        List<InetSocketAddress> addresses = register.lookup(serviceName);

        // 2.将连接缓存
        addresses.forEach(address -> {

                try {
                    if(!LrpcBootstrap.CHANNEL_CACHE.containsKey(address)) {
                        Channel channel = NettyBootStrapInitializer.getBootstrap().connect(address).sync().channel();
                        LrpcBootstrap.CHANNEL_CACHE.put(address,channel);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
        });

        log.info("----------------------定时任务----------------------");
        // 3.定时任务，定期发送消息(但是我们不能让主线程阻塞在这里去发心跳请求，我们先另外开一个新线程去处理这个)
        Thread thread = new Thread(() -> new Timer().scheduleAtFixedRate(new MyTimerTask(), 0, 2000), "lrpc-heartbeatDetector-thread");
        thread.setDaemon( true);//设置为守护线程
        thread.start();


    }

    public static class MyTimerTask extends TimerTask {
        @Override
        public void run() {

            //todo 这里所有的线程都会操作同一个ANSWER_TIME_CHANNEL_CACHE

            // 将响应时长的map进行清空(如果不清空：旧数据会一直累积，影响实时性判断。)(是的我们每次发起请求都要重新记录一下每个节点的响应时间，之前的响应时间map删了就行)
            LrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.clear();

            Map<InetSocketAddress, Channel> channelCache = LrpcBootstrap.CHANNEL_CACHE;
            for(Map.Entry<InetSocketAddress,Channel> entry: channelCache.entrySet()){
                Channel channel = entry.getValue();

                long start = System.currentTimeMillis();
                LrpcRequest lrpcRequest = LrpcRequest.builder()
                        .requestId(LrpcBootstrap.ID_GENERATOR.getId())
                        .compressType(CompressFactory.getStringCompressWrapper(LrpcBootstrap.COMPRESS_TYPE).getCode())
                        .serializeType(SerializerFactory.getStringSerialize(LrpcBootstrap.SERIALIZE_TYPE).getCode())
                        .requestType(RequestType.HEART_BEAT.getId())
                        .build();

                // 4.写出报文
                CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                // 将completableFuture暴露出去
                LrpcBootstrap.PENDING_REQUESTS.put(lrpcRequest.getRequestId(),completableFuture);

                channel.writeAndFlush(lrpcRequest)
                        .addListener((ChannelFutureListener) promise -> {
                            if (!promise.isSuccess()) {
                                completableFuture.completeExceptionally(promise.cause());
                            }
                        });
                Long end = null;
                try {
                    completableFuture.get();
                    end = System.currentTimeMillis();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }

                Long time = end - start;
                // 使用treeMap进行缓存(自动排序)
                LrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.put(time, channel);
                log.debug("和【{}】服务的响应的时间是【{}】",entry.getKey(),time);

            }

            for(Map.Entry<Long,Channel> node:LrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.entrySet()){
                if(log.isDebugEnabled()){
                    log.debug("和【{}】服务的响应时间是【{}】",node.getValue().remoteAddress(),node.getKey());
                }
            }
        }
    }

}

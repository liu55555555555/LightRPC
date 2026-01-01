package com.Lrpc.watch;

import com.Lrpc.LrpcBootstrap;
import com.Lrpc.NettyBootStrapInitializer;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

@Slf4j
public class UpAndDownWatcher implements Watcher {
    @Override
    public void process(WatchedEvent watchedEvent) {
        if(log.isDebugEnabled()){
            log.debug("【{}】服务列表发生改变（有节点上/下线），将重新拉取服务列表", watchedEvent.getPath());
        }
        List<InetSocketAddress> addresses = LrpcBootstrap.getInstance().getRegister().lookup(getServiceName(watchedEvent.getPath()));
        // 新增的节点 在address中，不在CHANNEL_CACHE中
        // 下线的节点 可能会在CHANNEL_CACHE中，不在address中

        // 处理上线的节点
        for(InetSocketAddress address:addresses){
            if (!LrpcBootstrap.CHANNEL_CACHE.containsKey(address)){
                Channel channel = null;
                try {
                    channel = NettyBootStrapInitializer.getBootstrap().connect(address).sync().channel();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                LrpcBootstrap.CHANNEL_CACHE.put(address,channel);
            }

        }

        // 处理下线的节点
        for(Map.Entry<InetSocketAddress,Channel> entry: LrpcBootstrap.CHANNEL_CACHE.entrySet()){
            InetSocketAddress key = entry.getKey();
            if (!addresses.contains(key)){
                Channel channel = entry.getValue();
                // 先关闭channel，然后再从缓存中移除
                if (channel != null && channel.isActive()) {
                    try {
                        channel.close().sync();
                        if(log.isDebugEnabled()){
                            log.debug("关闭下线节点【{}】的连接通道", key);
                        }
                    } catch (InterruptedException e) {
                        log.error("关闭下线节点【{}】的连接通道时发生异常", key, e);
                    }
                }
                // 从缓存中移除
                LrpcBootstrap.CHANNEL_CACHE.remove(key);
            }
        }

        // 重新加载负载均衡器
        LrpcBootstrap.LOAD_BALANCER.reLoadBalance(getServiceName(watchedEvent.getPath()),addresses);

    }

    private String getServiceName(String path) {
        String[] split = path.split("/");
        return split[split.length-1];
    }
}

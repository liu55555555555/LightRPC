package com.Lrpc.loadbalancer;

import com.Lrpc.LrpcBootstrap;
import com.Lrpc.transport.message.LrpcRequest;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * 最短响应时间负载均衡
 */
@Slf4j
public class MinTimeBalancer extends  AbstractLoadBalancer{

    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new MinTimeSelector(serviceList);
    }

    private static class MinTimeSelector implements Selector{
        public MinTimeSelector(List<InetSocketAddress> servers ) {
        }

        /**
         * 通过最短响应时间算法拿到一个服务节点
         * @return
         */
        @Override
        public InetSocketAddress getNext() {
            Map.Entry<Long, Channel> entry = LrpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.firstEntry();
            if(entry!=null ){
                return (InetSocketAddress)entry.getValue().remoteAddress();
            }
            //直接从现有的服务节点的缓存中拿一个
            return LrpcBootstrap.CHANNEL_CACHE.entrySet().iterator().next().getKey();
        }

        @Override
        public void reBalance() {

        }




    }

}

package com.Lrpc.loadbalancer;

import com.Lrpc.Exception.LoadBalancerException;
import com.Lrpc.LrpcBootstrap;
import com.Lrpc.discovery.Registry;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;



/**
 * 轮询负载均衡
 */
@Slf4j
public class RoundRobinLoadBalancer extends  AbstractLoadBalancer{

    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new RoundRobinSelector(serviceList);
    }

    /**
     *  轮询选择器。
     */
    private static class RoundRobinSelector implements Selector{

        // 维护的服务列表
        private List<InetSocketAddress> servers;
        private AtomicInteger index;

        public RoundRobinSelector(List<InetSocketAddress> servers) {
            this.servers = servers;
            this.index = new AtomicInteger(0);
        }

        /**
         * 通过轮询算法拿到一个服务节点
         * @return
         */
        @Override
        public InetSocketAddress getNext() {

            if( servers == null || servers.size() == 0){
                log.error("进行负载均衡的服务列表为空");
                throw new LoadBalancerException();
            }

            InetSocketAddress inetSocketAddress = servers.get(index.get());

            if(index.get() == servers.size()-1){
                index.set(0);
            }else{
                index.getAndIncrement();
            }



            return inetSocketAddress;
        }

        @Override
        public void reBalance() {

        }
    }

}

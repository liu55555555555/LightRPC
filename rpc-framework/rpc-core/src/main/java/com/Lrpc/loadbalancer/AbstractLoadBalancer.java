package com.Lrpc.loadbalancer;

import com.Lrpc.LrpcBootstrap;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模板方法模式
 */
public abstract class AbstractLoadBalancer implements LoadBalancer{

    // 一个服务会匹配一个selector
    private Map<String,Selector> cache = new ConcurrentHashMap<>(8);

    @Override
    public InetSocketAddress selectServiceAddress(String serviceName) {

        // 1.优先从缓存中获取selector
        Selector selector = cache.get(serviceName);


        // 2.如果没有，则为当前服务创建一个selector并放入缓存
        if(selector == null){
            // 对于这个负载均衡器，内部应该维护一个服务列表作为缓存
            List<InetSocketAddress> serviceList = LrpcBootstrap.getInstance().getRegister().lookup(serviceName);

            // 提供一些算法负责选取合适的结点
            selector = getSelector(serviceList);

            cache.put(serviceName,selector);
        }
        // 获取可用结点
        return selector.getNext();

    }

    /**
     * 由子类进行扩展
     * @param serviceList
     * @return
     */
    protected abstract Selector getSelector(List<InetSocketAddress> serviceList);
}

package com.Lrpc.loadbalancer;

import java.net.InetSocketAddress;

/**
 * selector封装的是获取一个服务节点的算法
 */
public interface Selector {

    /**
     * 根据服务列表依据某种算法获取一个服务节点
     * @return 具体的服务结点
     */
    InetSocketAddress getNext();


}

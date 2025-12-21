package com.Lrpc.loadbalancer;

import java.net.InetSocketAddress;

public interface LoadBalancer {

    // 这个接口具备的能力，才是根据服务名拉取服务列表然后进行列表缓存，然后返回一个可用的服务

    /**
     * 根据服务名获取一个可用的服务
     * @param serviceName
     * @return 服务地址
     */
     InetSocketAddress selectServiceAddress(String serviceName);

}

package com.Lrpc.discovery;

import com.Lrpc.ServiceConfig;

import java.net.InetSocketAddress;

public interface Registry {

    /**
     * 将服务注册到注册中心
     * @param serviceConfig
     */
    void register(ServiceConfig<?> serviceConfig);

    /**
     * 从注册中心拉取一个可用的服务
     * @param serviceName 服务的名称
     * @return 服务的地址
     */
    InetSocketAddress lookup(String serviceName);
}

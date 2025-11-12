package com.Lrpc.discovery;

import com.Lrpc.Constant;
import com.Lrpc.Exception.DiscoveryException;
import com.Lrpc.discovery.impl.NocasRegistry;
import com.Lrpc.discovery.impl.ZookeeperRegistry;

/**
 * 生产registry实现的工厂
 */
public class RegistryConfig {

    //定义链接的 url zookeeper://127.0.0.1:2181 redis://192.168.12.134:6379
    private String connectStringName;

    public RegistryConfig(String connectStringName) {
        this.connectStringName = connectStringName;
    }

    //使用简单工厂来完成
    //todo:优化为工厂方法模式（zookeeper工厂，redis工厂，每个不同的注册中心都有用一个工厂）
    public Registry getRegistry() {
        //1.拿到注册中心类型
        String registryType = getRegistryTypeOrHost(connectStringName,true).toLowerCase().trim();//toLowerCase().trim():转为小写，去除收尾空白
        if(registryType.equals("zookeeper") ){
            String host = getRegistryTypeOrHost(connectStringName,false);
            return new ZookeeperRegistry(host, Constant.DEFAULT_ZK_TIMEOUT);//"127.0.0.1:2181" 只有host:port，这样子也可以链接zookeeper
        }else if(registryType.equals("nacos")){
            String host = getRegistryTypeOrHost(connectStringName,false);
            return new NocasRegistry(host, Constant.DEFAULT_ZK_TIMEOUT);
        }
        throw new DiscoveryException("未发现合适的注册中心");
    }


    private String getRegistryTypeOrHost(String connectStringName,boolean isType){
        String[] typeAndHost = connectStringName.split("://");
        if(typeAndHost.length != 2){
            throw new IllegalArgumentException("给定的注册中心url不合法");
        }
        if(isType){
            return typeAndHost[0];
        }else {
            return typeAndHost[1];
        }

    }

}

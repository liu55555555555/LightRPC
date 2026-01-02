package com.Lrpc.discovery.impl;

import com.Lrpc.Constant;
import com.Lrpc.Exception.DiscoveryException;
import com.Lrpc.LrpcBootstrap;
import com.Lrpc.ServiceConfig;
import com.Lrpc.discovery.AbstractRegistry;
import com.Lrpc.discovery.Registry;
import com.Lrpc.utils.NetUtils;
import com.Lrpc.utils.zookeeper.ZookeeperNode;
import com.Lrpc.utils.zookeeper.ZookeeperUtils;
import com.Lrpc.watch.UpAndDownWatcher;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;
import java.util.List;


public class ZookeeperRegistry  extends AbstractRegistry implements Registry {

    private ZooKeeper zookeeper;

    public ZookeeperRegistry() {
        this.zookeeper = ZookeeperUtils.createZookeeper();
    }
    public ZookeeperRegistry(String connectectString,int timeOut) {
        this.zookeeper = ZookeeperUtils.createZookeeper(connectectString,timeOut);
    }

    /**
     * 将服务注册到注册中心
     * @param service
     */
    @Override
    public void register(ServiceConfig<?> service) {
        //服务名称的节点(持久节点)（上一级的基础目录结构在rpc-manager中创建）
        String parentNode = Constant.BASE_PROVIDERS_PATH + "/" + service.getInterfaceClass().getName();

        if(!ZookeeperUtils.exists(zookeeper, parentNode,null)){
            ZookeeperUtils.createNode(zookeeper, new ZookeeperNode(parentNode, null),null, CreateMode.PERSISTENT);
        }

        //创建本机的临时节点,ip:port
        //服务提供方的端口一般自己设定，我们还需要一个获取ip的方法
        //ip我们通常时需要一个局域网ip，不是127.0.0.1，也不是ipv6
        //需要像192.168.12.123这样的
        //todo：全局端口处理
        ZookeeperNode node = new ZookeeperNode(parentNode + "/" + NetUtils.getIp() + ":" + LrpcBootstrap.getInstance().getConfiguration().getPort(), null);
        if(!ZookeeperUtils.exists(zookeeper, node.getPath(),null)){
            ZookeeperUtils.createNode(zookeeper, node,null, CreateMode.EPHEMERAL);
        }

    }


    /**
     * 注册中心的核心目的是：拉取合适的服务列表（一个服务会注册多个服务列表）
     * @param serviceName 服务的名称
     * @return 返回服务列表
     */
    @Override
    public List<InetSocketAddress> lookup(String serviceName) {
        //1.找到服务对应的节点
        String servicePath = Constant.BASE_PROVIDERS_PATH + "/" + serviceName;
        //2.从zk中获取子节点
        List<String> children = ZookeeperUtils.getChildren(zookeeper, servicePath, new UpAndDownWatcher());
        //3.获取了所有的可用的服务列表，封装结果
        List<InetSocketAddress> inetSocketAddresses = children.stream().map(ipAndPort -> {
            String[] split = ipAndPort.split(":");
            String ip = split[0];
            int port = Integer.parseInt(split[1]);
            return new InetSocketAddress(ip, port);
        }).toList();//toList()相当于创建（new）了一个列表对象，故不能用null来判断是否为空，需判断集合元素数是否为0
        if(inetSocketAddresses.isEmpty()){
            throw new DiscoveryException("未发现任何可用的服务主机");
        }

        return inetSocketAddresses;
    }
}

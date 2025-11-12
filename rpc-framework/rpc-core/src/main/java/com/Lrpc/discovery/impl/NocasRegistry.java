package com.Lrpc.discovery.impl;

import com.Lrpc.Constant;
import com.Lrpc.ServiceConfig;
import com.Lrpc.discovery.AbstractRegistry;
import com.Lrpc.discovery.Registry;
import com.Lrpc.utils.NetUtils;
import com.Lrpc.utils.zookeeper.ZookeeperNode;
import com.Lrpc.utils.zookeeper.ZookeeperUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;

public class NocasRegistry  extends AbstractRegistry implements Registry {
        private ZooKeeper zookeeper;

    public NocasRegistry() {
            this.zookeeper = ZookeeperUtils.createZookeeper();
        }
    public NocasRegistry(String connectectString,int timeOut) {
            this.zookeeper = ZookeeperUtils.createZookeeper(connectectString,timeOut);
        }

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
            ZookeeperNode node = new ZookeeperNode(parentNode + "/" + NetUtils.getIp() + ":" + 8080, null);
            if(!ZookeeperUtils.exists(zookeeper, node.getPath(),null)){
                ZookeeperUtils.createNode(zookeeper, node,null, CreateMode.EPHEMERAL);
            }

        }

    @Override
    public InetSocketAddress lookup(String serviceName) {
        return null;
    }
}

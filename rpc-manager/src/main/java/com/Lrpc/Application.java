package com.Lrpc;

import com.Lrpc.utils.zookeeper.ZookeeperNode;
import com.Lrpc.utils.zookeeper.ZookeeperUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;


import java.util.List;

/**
 * 注册中心的管理页面
 */
@Slf4j
public class Application {
    public static void main(String[] args) {
        //此main方法的作用：帮我们创建zookeeper的基础目录，而且是持久化节点不需要设置监听（基本目录持久化）
//Lrpc-metadata （持久节点）
//  └ providers （持久节点, 接口的全限定名）
//      └ service1  [data] /ip:port
//          ├── node1 [data]
//          ├── node2 [data]
//          └── node3 [data]
//  └ consumers
//      └ service1
//          ├── node1 [data]
//          ├── node2 [data]
//          └── node3 [data]
//  └ config

        //DeepSeek推荐：
        // 推荐的 ZooKeeper 目录结构：
        // /lrpc-metadata (持久节点)
        //   ├── providers (持久节点)
        //   │   └── com.lightrpc.api.sayHello (持久节点, 接口全限定名)
        //   │       ├── 192.168.1.100:8080 (临时节点, 数据: 序列化后的服务元数据)
        //   │       ├── 192.168.1.101:8080 (临时节点, 数据: 序列化后的服务元数据)
        //   │       └── 192.168.1.102:8080 (临时节点, 数据: 序列化后的服务元数据)
        //   ├── consumers (持久节点)
        //   │   └── com.lightrpc.api.sayHello (持久节点)
        //   │       ├── consumer-1 (临时节点, 数据: 消费者元数据)
        //   │       └── consumer-2 (临时节点, 数据: 消费者元数据)
        //   └── config (持久节点)
        //       ├── global (持久节点, 全局配置)
        //       └── com.lightrpc.api.sayHello (持久节点, 服务特定配置)


        //定义节点和数据
//        String basePath = "/lrpc-metadata";
//        String providerPath = basePath+"/providers";
//        String consumersPath = basePath+"/consumers";

        ZookeeperNode zookeeperNode1 = new ZookeeperNode(Constant.BASE_PATH,null);
        ZookeeperNode zookeeperNode2= new ZookeeperNode(Constant.BASE_PROVIDERS_PATH,null);
        ZookeeperNode zookeeperNode3 = new ZookeeperNode(Constant.BASE_CONSUMERS_PATH,null);


        //创建zookeeper实例
        ZooKeeper zooKeeper = ZookeeperUtils.createZookeeper();

        List.of(zookeeperNode1,zookeeperNode2,zookeeperNode3).forEach(zookeeperNode -> {
            ZookeeperUtils.createNode(zooKeeper,zookeeperNode,null, CreateMode.PERSISTENT);
        });

        ZookeeperUtils.close(zooKeeper);


    }
}






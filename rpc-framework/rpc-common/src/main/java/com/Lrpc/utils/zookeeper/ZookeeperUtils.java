package com.Lrpc.utils.zookeeper;

import com.Lrpc.Constant;
import com.Lrpc.Exception.ZookeeperException;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class ZookeeperUtils {


    /**
     * 使用默认配置创建zookeeper实例
     * @return zooKeeper实例
     */
    public static ZooKeeper createZookeeper(){
        //构造函数所需参数：String connectString, int sessionTimeout, Watcher watcher
        //定义链接参数
        String connectString = Constant.DEFAULT_ZK_CONNECT;
        //定义超时时间
        int timeout = Constant.DEFAULT_ZK_TIMEOUT;

        return createZookeeper(connectString,timeout);

    }


    public static ZooKeeper createZookeeper(String connectString,int  timeout){
        CountDownLatch countDownLatch = new CountDownLatch(1);

        try {
            //创建Zookeeper实例，建立链接
            final ZooKeeper zooKeeper = new ZooKeeper(connectString, timeout, event -> {
                //只有链接成功才放行
                if(event.getState() == Watcher.Event.KeeperState.SyncConnected){
                    if(log.isDebugEnabled()){
                        log.debug("客户端已连接成功");
                    }
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await();

            return zooKeeper;

        } catch (IOException | InterruptedException e) {
            log.error("创建zookeeper实例时发生异常",e);
            throw new ZookeeperException();
        }
    }

    /**
     * 创建一个zookeeper节点的工具方法
     * @param zooKeeper zookeeper实例
     * @param node 节点
     * @param watcher watcher实例
     * @param mode 节点的类型
     * @return true:创建成功 false:已存在 异常：直接抛出
     */
    public static boolean createNode(ZooKeeper zooKeeper,ZookeeperNode node,Watcher watcher,CreateMode mode){
            try {
                if(zooKeeper.exists(node.getPath(), watcher) == null){
                    String ret = zooKeeper.create(node.getPath(), node.getData(),
                            ZooDefs.Ids.OPEN_ACL_UNSAFE, mode);
                    log.info("创建节点成功，节点路径为：{}",ret);
                    return true;
                }else{
                    if(log.isDebugEnabled()){
                        log.info("节点【{}】已存在,无需再创建",node.getPath());
                    }
                    return false;
                }
            } catch (KeeperException | InterruptedException e) {
                log.error("创建基础目录时发生异常：" ,e);
                throw new ZookeeperException(e);
            }
    }

    /**
     * 判断节点是否存在
     * @param zooKeeper zookeeper实例
     * @param path 节点路径
     * @param watcher watcher实例
     * @return true:存在 false:不存在
     */
    public static boolean exists(ZooKeeper zooKeeper,String path,Watcher watcher){
        try {
            return zooKeeper.exists(path, watcher) != null;
        } catch (KeeperException | InterruptedException e) {
            log.error("查询节点[{}]时发生异常",path,e);
            throw new ZookeeperException(e);
        }
    }


    /**
     * 查询一个节点的子元素
     * @param zooKeeper zookeeper实例
     * @param path 节点路径
     * @return  子元素列表
     */
    public static List<String> getChildren(ZooKeeper zooKeeper, String path, Watcher watcher){
        try {
            return zooKeeper.getChildren(path, watcher);
        } catch (KeeperException | InterruptedException e) {
            log.error("查询节点[{}]时发生异常",path,e);
            throw new ZookeeperException(e);
        }
    }



    /**
     * 关闭一个zookeeper实例
     * @param zooKeeper
     */
    public static void close(ZooKeeper zooKeeper ){
        try {
            zooKeeper.close();
        } catch (InterruptedException e) {
            log.error("关闭zookeeper实例时发生异常",e);
            throw new ZookeeperException();
        }
    }





}

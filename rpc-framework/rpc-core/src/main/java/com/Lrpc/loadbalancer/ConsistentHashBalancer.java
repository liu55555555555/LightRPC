package com.Lrpc.loadbalancer;

import com.Lrpc.Exception.LoadBalancerException;
import com.Lrpc.LrpcBootstrap;
import com.Lrpc.transport.message.LrpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 一致性哈希负载均衡
 */
@Slf4j
public class ConsistentHashBalancer extends  AbstractLoadBalancer{

    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new ConsistentSelector(serviceList,128);
    }

    /**
     *  一致性哈希选择器。
     */
    private static class ConsistentSelector implements Selector{

        // hash环用来存储服务器节点
        private SortedMap<Integer, InetSocketAddress> circle = new TreeMap<>();
        // 虚拟节点的个数
        private int virtualNodeNum;

        // 构造方法做的事: 初始化服务节点(添加节点到hash环上并且添加虚拟节点)
        public ConsistentSelector(List<InetSocketAddress> servers, int virtualNodeNum) {
            this.virtualNodeNum = virtualNodeNum;

            // 把每一个节点加到hash环上
            for(InetSocketAddress server : servers){
                addNodeToCircle(server);
            }

        }

        /**
         * 通过一致性哈希算法拿到一个服务节点
         * @return
         */
        @Override
        public InetSocketAddress getNext() {
            // 1.当前的hash环已经建立好了，接下来需要对请求的要素做处理:我们应该选择什么要素来进行hash运算呢，以至于可以拿到一个可用的服务节点
            // 这里我们考虑使用请求的id作为要素去获取服务节点 但是我们要怎么获取这个请求的id要素呢 ----> ThreadLocal (我们每封装建立好一个请求后就把这个请求存入ThreadLocal)
            LrpcRequest lrpcRequest = LrpcBootstrap.REQUEST_THREAD_LOCAL.get();
            String id = Long.toString(lrpcRequest.getRequestId());

            // 请求的id做hash(这里不使用String默认的Hash算法：因为默认的算法计算出来的值是连续。我们想要的是分散的的hash值去打到整个服务列表环上，这样才能达到负载均衡的效果)
            int hash = hash(id);

            // 判断hash值是否能直接落在一个服务器上(即:和某个服务器的hash一样)
            if(!circle.containsKey(hash)){
                // 如果这个请求的id的hash没有直接打在服务节点环上，那么就从hash环上顺时针找到最近的一个节点
                SortedMap<Integer, InetSocketAddress> tailMap = circle.tailMap(hash);
                hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
            }


            return circle.get(hash);
        }


        // --------------------------------------工具类方法--------------------------------------

        /**
         * 添加节点到hash环上
         * @param server 节点的地址
         */
        private void addNodeToCircle(InetSocketAddress server) {
            // 为每一个节点生成匹配的虚拟节点进行挂载
            for(int i = 0; i < virtualNodeNum; i++){
                int hash = hash(server.toString() + "-"+i);
                // 把虚拟节点挂载到hash环上
                circle.put(hash, server);
                if(log.isDebugEnabled()){
                    log.debug("添加服务节点：{},hash为:[{}]", server,hash);
                }
            }
        }


        /**
         * 从hash环中删除节点
         * @param server 节点的地址
         */
        private void removeNodeFromCircle(InetSocketAddress server) {
            for(int i = 0; i < virtualNodeNum; i++){
                int hash = hash(server.toString() + "-"+i);
                circle.remove(hash, server);
            }
        }

        /**
         *  具体的hash算法（将服务节点映射为hash值）
         * @param s
         * @return
         */
        private int hash(String s) {
            MessageDigest md;

            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            byte[] digest = md.digest(s.getBytes());
            //md5得到的结果是一个字节数组，但是我们想要int 4个字节
            int ret=0;
            for (int i= 0;i < 4;i++){
                ret = ret << 8;
                ret = ret | digest[i] & 0xFF;//& 0xFF:改善hash值，避免大部分hash值都是负数
            }

            return ret;
        }



    }

}

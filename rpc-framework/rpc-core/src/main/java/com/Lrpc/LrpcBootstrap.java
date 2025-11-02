package com.Lrpc;

import lombok.extern.slf4j.Slf4j;

import java.lang.module.ResolvedModule;
import java.util.List;
import java.util.concurrent.Phaser;
import java.util.logging.Handler;

@Slf4j
public class LrpcBootstrap {

    //构造器私有化
    private LrpcBootstrap(){
        //构造启动引导程序时需要做一些什么初始化的事
    }

    //LrpcBootstrap是个单例，我们希望每个应用程序只有一个实例
    private static LrpcBootstrap lrpcBootstrap=new LrpcBootstrap();
    public static LrpcBootstrap getInstance() {
        return lrpcBootstrap;
    }

    /**
     * 用来定义当前应用的名字
     * @param appName
     * @return this当前实例
     */
    public LrpcBootstrap application(String appName) {
        return this;
    }

    /**
     * 配置一个注册中心
     * @param registryConfig 注册中心
     * @return this当前实例
     */
    public LrpcBootstrap register(RegistryConfig registryConfig) {
        return this;
    }


    /**
     * 配置当前暴露的服务使用的协议
     * @param protocolConfig
     * @return this当前实例
     */
    public LrpcBootstrap protocol(ProtocolConfig protocolConfig) {
        if(log.isDebugEnabled()){
            log.debug("当前工程使用了：{}协议进行序列化",protocolConfig.getProtocolName());
        }
        return this;
    }


    /**
     * =======================================服务提供方的相关api============================================
     */

    /**
     * 发布服务，将接口实现，注册到服务中心
     * @param service 封装的需要发布的服务
     * @return
     */
    public LrpcBootstrap publish(ServiceConfig<?> service) {
        if(log.isDebugEnabled()){
            log.debug("服务：{}已被注册",service.getInterfaceClass().getName());
        }
        return this;
    }

    /**
     * 批量发布服务
     * @param services 封装的需要发布的服务集合
     * @return
     */
    public LrpcBootstrap publish(List<ServiceConfig<?>> services) {
        return this;
    }

    /**
     * 启动netty服务
     */
    public void start() {
    }



    /**
     * =======================================调用方的相关api============================================
     */
    public LrpcBootstrap reference(ReferenceConfig<?> reference) {
        if(log.isDebugEnabled()){
            log.debug("服务：{}已被引用",reference.getInterfaceClass().getName());
        }

        //在这个方法里我们是否可以拿到相关的配置项-注册中心
        //配置reference，将来调用get方法时，方便生成代理对象
        return this;

    }


}

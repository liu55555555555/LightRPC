package com.Lrpc;

public class Constant {

    //zookeeper的默认链接地址
    public static final String DEFAULT_ZK_CONNECT = "127.0.0.1:2181";
    //zookeeper的默认超时时间
    public static final int DEFAULT_ZK_TIMEOUT = 10000;


    //服务调用方和服务提供方在注册中心的基础路径
    public static final String BASE_PATH ="/lrpc-metadata";
    public static final String BASE_PROVIDERS_PATH =BASE_PATH + "/providers";
    public static final String BASE_CONSUMERS_PATH =  BASE_PATH + "/consumers";

}

package com.Lrpc.serialize;

/**
 * 序列化器
 */
public interface Serialize {


    /**
     * 序列化的方法
     * @param obj 需要进行序列化的对象
     * @return 序列化之后的字节数组
     * */
    byte[] serialize(Object  obj);


    /**
     * 反序列化的方法
     * @param bytes 待反序列化的字节数组
     * @param clazz 目标对象类型
     * @return 反序列化之后的对象
     * @param <T> 目标对象类型
     */
    <T> T deserialize(byte[] bytes,Class<T> clazz);

}

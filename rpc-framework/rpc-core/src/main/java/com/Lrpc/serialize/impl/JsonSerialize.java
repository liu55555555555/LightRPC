package com.Lrpc.serialize.impl;

import com.Lrpc.Exception.SerializeException;
import com.Lrpc.serialize.Serialize;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class JsonSerialize implements Serialize {

    @Override
    public byte[] serialize(Object obj) {
        if(obj == null){
            return null;
        }

        byte[] jsonBytes = JSON.toJSONBytes(obj);

        if(log.isDebugEnabled()){
            log.debug("json序列化【{}】成功，序列化后的字节数为：【{}】", obj, jsonBytes.length);
        }

        return jsonBytes;

    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if(bytes == null || bytes.length == 0){
            return null;
        }

        T t = JSON.parseObject(bytes, clazz);

        if(log.isDebugEnabled()){
            log.debug("json反序列化【{}】成功", clazz);
        }

        return t;
    }
}

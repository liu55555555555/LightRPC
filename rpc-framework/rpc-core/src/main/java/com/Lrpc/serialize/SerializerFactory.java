package com.Lrpc.serialize;

import com.Lrpc.serialize.impl.HessianSerialize;
import com.Lrpc.serialize.impl.JdkSerialize;
import com.Lrpc.serialize.impl.JsonSerialize;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
// todo 修改完善工厂方法
public class SerializerFactory {
    private static final ConcurrentHashMap<String,SerializeWrapper> STRING_SERIALIZE_WRAPPER_CACHE = new ConcurrentHashMap<>(8);
    private static final ConcurrentHashMap<Byte,SerializeWrapper> BYTE_SERIALIZE_WRAPPER_CACHE = new ConcurrentHashMap<>(8);
    static{
        STRING_SERIALIZE_WRAPPER_CACHE.put("jdk",new SerializeWrapper(new JdkSerialize(),(byte)1,"jdk"));
        STRING_SERIALIZE_WRAPPER_CACHE.put("json",new SerializeWrapper(new JsonSerialize(),(byte)2,"json"));
        STRING_SERIALIZE_WRAPPER_CACHE.put("hessian",new SerializeWrapper(new HessianSerialize(),(byte)3,"hessian"));

        BYTE_SERIALIZE_WRAPPER_CACHE.put((byte)1,STRING_SERIALIZE_WRAPPER_CACHE.get("jdk"));
        BYTE_SERIALIZE_WRAPPER_CACHE.put((byte)2,STRING_SERIALIZE_WRAPPER_CACHE.get("json"));
        BYTE_SERIALIZE_WRAPPER_CACHE.put((byte)3,STRING_SERIALIZE_WRAPPER_CACHE.get("hessian"));
    }

    public static SerializeWrapper getStringSerialize(String serializeName){

        SerializeWrapper serializeWrapper = STRING_SERIALIZE_WRAPPER_CACHE.get(serializeName);
        if(serializeWrapper == null){
            log.warn("未找到对应的序列化方式【{}】，使用默认的【{}】",serializeName,"hessian");
            return STRING_SERIALIZE_WRAPPER_CACHE.get("hessian");
        }

        return serializeWrapper;
    }

    public static SerializeWrapper getByteSerialize(byte serializeCode){
        SerializeWrapper serializeWrapper = BYTE_SERIALIZE_WRAPPER_CACHE.get(serializeCode);
        if(serializeWrapper == null){
            log.warn("未找到对应编号的序列化方式【{}】，使用默认的【{}】",serializeCode,"hessian");
            return BYTE_SERIALIZE_WRAPPER_CACHE.get((byte)3);
        }
        return serializeWrapper;
    }

}

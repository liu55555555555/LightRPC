package com.Lrpc.serialize;

import com.Lrpc.serialize.impl.HessianSerialize;
import com.Lrpc.serialize.impl.JdkSerialize;
import com.Lrpc.serialize.impl.JsonSerialize;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
        return STRING_SERIALIZE_WRAPPER_CACHE.get(serializeName);
    }

    public static SerializeWrapper getByteSerialize(byte serializeCode){
        return BYTE_SERIALIZE_WRAPPER_CACHE.get(serializeCode);
    }
}

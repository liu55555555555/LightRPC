package com.Lrpc.compress;

import com.Lrpc.compress.Impl.GzipCompressor;
import com.Lrpc.serialize.SerializeWrapper;
import com.Lrpc.serialize.impl.HessianSerialize;
import com.Lrpc.serialize.impl.JdkSerialize;
import com.Lrpc.serialize.impl.JsonSerialize;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
@Slf4j
// todo 修改完善工厂方法
public class CompressFactory {
    private static final ConcurrentHashMap<String, CompressWrapper> STRING_COMPRESS_WRAPPER_CACHE = new ConcurrentHashMap<>(8);
    private static final ConcurrentHashMap<Byte,CompressWrapper> BYTE_COMPRESS_WRAPPER_CACHE = new ConcurrentHashMap<>(8);
    static{
        STRING_COMPRESS_WRAPPER_CACHE.put("gzip",new CompressWrapper((byte)1,"gzip",new GzipCompressor()));

        BYTE_COMPRESS_WRAPPER_CACHE.put((byte)1,STRING_COMPRESS_WRAPPER_CACHE.get("gzip"));

    }

    public static CompressWrapper getStringCompressWrapper(String compressName){
        CompressWrapper compressWrapper = STRING_COMPRESS_WRAPPER_CACHE.get(compressName);
        if(compressWrapper == null){
            log.warn("未找到对应的压缩方式【{}】，使用默认的【{}】",compressName,"GZIP");
            return STRING_COMPRESS_WRAPPER_CACHE.get("gzip");
        }

        return compressWrapper;
    }

    public static CompressWrapper getByteCompressWrapper(byte serializeCode){

        CompressWrapper compressWrapper = BYTE_COMPRESS_WRAPPER_CACHE.get(serializeCode);

        if(compressWrapper == null){
            log.warn("未找到对应编码的压缩方式【{}】，使用默认的【{}】",serializeCode,"GZIP");
            return BYTE_COMPRESS_WRAPPER_CACHE.get((byte)1);
        }
        return compressWrapper;
    }

}

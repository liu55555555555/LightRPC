package com.Lrpc.serialize.impl;

import com.Lrpc.Exception.SerializeException;
import com.Lrpc.serialize.Serialize;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class HessianSerialize implements Serialize {

    /**
     * 序列化
     * @param obj 需要进行序列化的对象
     * @return
     */
    @Override
    public byte[] serialize(Object obj) {
        if(obj == null){
            return null;
        }

        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ) {
            Hessian2Output hessian2Output = new Hessian2Output(baos);
            hessian2Output.writeObject(obj);
            hessian2Output.flush();

            byte[] ret = baos.toByteArray();
            if(log.isDebugEnabled()){
                log.debug("hessian序列化【{}】成功，序列化后的字节数为：【{}】", obj, ret.length);
            }
            return ret;
        } catch (Exception e) {
            log.error("hessian序列化【{}】失败", obj);
            throw new SerializeException(e);
        }

    }

    /**
     * 反序列化
     * @param bytes 待反序列化的字节数组
     * @param clazz 目标对象类型
     * @return
     * @param <T>
     */
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if(bytes == null || bytes.length == 0 || clazz == null){
            return null;
        }
        try(ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ) {

            Hessian2Input hessian2Input = new Hessian2Input(bais);
            T t = (T)hessian2Input.readObject();
            if(log.isDebugEnabled()){
                log.debug("Hessian反序列化【{}】成功", clazz);
            }
            return t;
        } catch (IOException e) {
            log.error("Hessian反序列化【{}】失败", clazz);
            throw new RuntimeException(e);
        }
    }
}

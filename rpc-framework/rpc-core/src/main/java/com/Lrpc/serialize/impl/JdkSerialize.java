package com.Lrpc.serialize.impl;

import com.Lrpc.Exception.SerializeException;
import com.Lrpc.serialize.Serialize;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class JdkSerialize implements Serialize {

    @Override
    public byte[] serialize(Object obj) {
        if(obj == null){
            return null;
        }

        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(baos);) {

            objectOutputStream.writeObject(obj);

            byte[] ret = baos.toByteArray();
            if(log.isDebugEnabled()){
                log.debug("jdk序列化【{}】成功，序列化后的字节数为：【{}】", obj, ret.length);
            }
            return ret;
        } catch (Exception e) {
            log.error("jdk序列化【{}】失败", obj);
            throw new SerializeException(e);
        }

    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if(bytes == null || bytes.length == 0){
            return null;
        }
        try(ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream objectInputStream = new ObjectInputStream(bais);) {
            Object o = objectInputStream.readObject();
            if(log.isDebugEnabled()){
                log.debug("jdk反序列化【{}】成功", o);
            }
            return (T) o;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}

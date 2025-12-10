package com.Lrpc.compress.Impl;

import com.Lrpc.Exception.CompressException;
import com.Lrpc.compress.Compressor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Gzip压缩实现
 */
@Slf4j
public class GzipCompressor implements Compressor {

    @Override
    public byte[] compress(byte[] bytes) {

        try(ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bos);){

            gzipOutputStream.write(bytes);
            gzipOutputStream.finish();

            byte[] result = bos.toByteArray();

            if(log.isDebugEnabled()){
                log.debug("压缩前大小：【{}】",bytes.length);
                log.debug("压缩后大小：【{}】",result.length);
            }

            return result;
        } catch (IOException e) {
            log.error("压缩异常");
            throw new CompressException(e);
        }

    }

    @Override
    public byte[] decompress(byte[] bytes) {

        try(ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            GZIPInputStream gzipInputStream = new GZIPInputStream(bais);){

            byte[] result = gzipInputStream.readAllBytes();
            if (log.isDebugEnabled()){
                log.debug("解压前大小：【{}】",bytes.length);
                log.debug("解压后大小：【{}】",result.length);
            }
            return result;
        }catch (IOException e){
            log.error("解压异常");
            throw new CompressException(e);
        }

    }
}

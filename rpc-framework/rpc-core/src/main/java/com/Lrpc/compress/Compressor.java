package com.Lrpc.compress;

public interface Compressor {

    /**
     * 压缩
     * @param bytes 需要压缩的字节数组
     * @return 压缩后的字节数组
     */
    byte[] compress(byte[] bytes);


    /**
     * 解压
     * @param bytes 需要解压的字节数组
     * @return 解压后的字节数组
     */
    byte[] decompress(byte[] bytes);

}

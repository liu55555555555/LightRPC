package com.Lrpc.transport.message;

public class MessageFormatConstant {
    // 魔数值：这里弄成多字节的字节数组是为了后续方便校验，如果是单字节的话会容易协议冲突：别人的rpc用的协议也是你用的这个单字节的。
    public final static byte[] MAGIC = "lrpc".getBytes();
    public final static byte VERSION = 1;

    // 头部信息的长度（真正的请求体前面的所有东西所占的字节数）（到时候我们就可以用获取的完整的报文的总长度减去头部信息长度得到请求体的长度）
    public final static short HEADER_LENGTH = (byte)(MAGIC.length + 1 + 2 + 4 + 1 + 1 + 1  + 8);
    // 头部信息长度占用的字节数(头部信息长度这个字段占用的长度)
    public static final int HEADER_FIELD_LENGTH = 2;

    public final static int MAX_FRAME_LENGTH = 1024 * 1024;

    public static final int VERSION_LENGTH = 1;

    // 总长度这个字段占用的字节数
    public static final int FULL_FIELD_LENGTH = 4;

}

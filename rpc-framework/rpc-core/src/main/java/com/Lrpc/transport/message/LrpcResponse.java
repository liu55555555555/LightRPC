package com.Lrpc.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 服务调用放发送的请求的内容（Serializable是个标识接口，表示继承这个接口的类都可以进行序列化）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LrpcResponse implements Serializable {

    // 请求的id
    private long requestId;

    // 压缩的类型，序列化的方式
    private byte compressType;
    private byte serializeType;
    // 响应码 1:成功 2:失败
    private byte code;

    // 具体的消息体
    private Object responseBody;

}

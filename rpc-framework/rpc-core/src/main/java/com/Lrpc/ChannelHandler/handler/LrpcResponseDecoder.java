package com.Lrpc.ChannelHandler.handler;

import com.Lrpc.compress.CompressFactory;
import com.Lrpc.compress.Compressor;
import com.Lrpc.enumeration.RequestType;
import com.Lrpc.serialize.Serialize;
import com.Lrpc.serialize.SerializerFactory;
import com.Lrpc.transport.message.LrpcRequest;
import com.Lrpc.transport.message.LrpcResponse;
import com.Lrpc.transport.message.MessageFormatConstant;
import com.Lrpc.transport.message.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

@Slf4j
public class LrpcResponseDecoder extends LengthFieldBasedFrameDecoder {

    public LrpcResponseDecoder() {
        // 调用这个父类的构造方法为了：找到当前报文的总长度，截取报文，截取出来的报文我们可以进行解析
        super(
                // 最大帧的长度，超过这个maxFrameLength值会直接丢弃掉
                MessageFormatConstant.MAX_FRAME_LENGTH,
                // 长度字段的偏移量
                MessageFormatConstant.MAGIC.length + MessageFormatConstant.VERSION_LENGTH + MessageFormatConstant.HEADER_FIELD_LENGTH,
                // 长度字段的长度
                MessageFormatConstant.FULL_FIELD_LENGTH,
                // 负载的适配长度
                -(MessageFormatConstant.MAGIC.length + MessageFormatConstant.VERSION_LENGTH
                        + MessageFormatConstant.HEADER_FIELD_LENGTH + MessageFormatConstant.FULL_FIELD_LENGTH),
                0);

    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decode = super.decode(ctx, in);
        if(decode instanceof ByteBuf byteBuf){
            return decodeFrame(byteBuf);
        }
        return null;
    }
    private Object decodeFrame(ByteBuf byteBuf){

        log.debug("开始解码报文---------------------------------------------------------------------------------");

        // 1.解析魔术值
        byte[] magic = new byte[MessageFormatConstant.MAGIC.length];
        byteBuf.readBytes(magic);
        //检测 魔术值
        for (int i = 0; i < MessageFormatConstant.MAGIC.length; i++) {
            if(magic[i] != MessageFormatConstant.MAGIC[i]){
                throw new RuntimeException("无效的报文，获取的请求不合法，不是Lrpc的报文");
            }
        }

        // 2.解析版本号
        byte version = byteBuf.readByte();
        if(version > MessageFormatConstant.VERSION){
            throw new RuntimeException("获取的版本不被支持");
        }

        // 3、解析头部的长度
        short headLength = byteBuf.readShort();

        // 4、解析总长度
        int fullLength = byteBuf.readInt();

        // 5、响应码
        byte responseCode = byteBuf.readByte();

        // 6、序列化类型
        byte serializeType = byteBuf.readByte();

        // 7、压缩类型
        byte compressType = byteBuf.readByte();

        // 8、请求id
        long requestId = byteBuf.readLong();


        // 封装
        LrpcResponse lrpcResponse = LrpcResponse.builder()
                .requestId(requestId)
                .compressType(compressType)
                .serializeType(serializeType)
                .code(responseCode)
                .build();


//        // todo 心跳请求没有负载，此处可以判断并直接返回
//        if( requestType == RequestType.HEART_BEAT.getId()){
//            return lrpcRequest;
//        }

        // 9、请求体
        int bodyLength = fullLength - headLength;
        byte[] body = new byte[bodyLength];
        byteBuf.readBytes(body);

        //  10.解压缩
        Compressor compressor = CompressFactory.getByteCompressWrapper(compressType).getCompressor();
        body = compressor.decompress(body);

        // 11.反序列化
        Serialize serialize = SerializerFactory.getByteSerialize(serializeType).getSerialize();
        Object responseBody = serialize.deserialize(body, Object.class);
        lrpcResponse.setResponseBody(responseBody);

        if(log.isDebugEnabled()){
            log.debug("响应【{}】已经在客户端完成解码",lrpcResponse.getRequestId());
        }

        return lrpcResponse;
    }
}

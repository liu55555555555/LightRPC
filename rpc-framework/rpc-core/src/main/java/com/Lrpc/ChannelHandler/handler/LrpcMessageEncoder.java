package com.Lrpc.ChannelHandler.handler;

import ch.qos.logback.core.rolling.helper.Compressor;
import com.Lrpc.transport.message.LrpcRequest;
import com.Lrpc.transport.message.MessageFormatConstant;
import com.Lrpc.transport.message.RequestPayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 *
 * MessageToByteEncoder 又继承了ChannelOutboundHandlerAdapter，故这个类确实是一个出站的处理器
 *
 * 出站时，第一个经过的处理器
 */
@Slf4j
public class LrpcMessageEncoder extends MessageToByteEncoder<LrpcRequest>{
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, LrpcRequest lrpcRequest, ByteBuf byteBuf) throws Exception {
        // 4个字节的魔术值
        byteBuf.writeBytes(MessageFormatConstant.MAGIC);            //写入4字节 → 写指针移动到4
        // 一个字节的版本号
        byteBuf.writeByte(MessageFormatConstant.VERSION);           // 写入1字节 → 写指针移动到5
        //两个字节的头部长度
        byteBuf.writeShort(MessageFormatConstant.HEADER_LENGTH);    // 写入2字节 → 写指针移动到7
        //总长度不知道，因为不清楚body的长度:利用netty的writerIndex（）方法，这是一个写指针。byteBuf.writerIndex()：拿到当前写指针的位置，当之后处理完body后，知道了处理后的body的大小，我们在返回来这里填入数据
        byteBuf.writerIndex(byteBuf.writerIndex() + MessageFormatConstant.FULL_FIELD_LENGTH);//操作做了什么：byteBuf.writerIndex()不传参：获取写指针当前的位置：7,7+4==11，最外层的方法：writerIndex(11)，则将指针指向11的位置，相当于跳过了4个字节。含义：为长度字段预留4字节空间，但不写入实际数据
        // 3个类型
        byteBuf.writeByte(lrpcRequest.getRequestType());// 1.请求类型
        byteBuf.writeByte(lrpcRequest.getSerializeType());// 2.序列化类型
        byteBuf.writeByte(lrpcRequest.getCompressType());// 3.压缩类型
        // 8字节的请求id
        byteBuf.writeLong(lrpcRequest.getRequestId());
        //写入请求体
        byte[] bodyBytes = getBodyBytes(lrpcRequest.getRequestPayload());
        byteBuf.writeBytes(bodyBytes);


        // 重新处理报文的总长度（因为之前咱们直接跳过了一段，现在返回来去补充内容）
        // 先保存当前的写指针的位置
        int writerIndex = byteBuf.writerIndex();
        // 将写指针的位置移动到总长度的位置上
        byteBuf.writerIndex(MessageFormatConstant.MAGIC.length
                + MessageFormatConstant.VERSION_LENGTH + MessageFormatConstant.HEADER_FIELD_LENGTH
        );
        byteBuf.writeInt(MessageFormatConstant.HEADER_LENGTH + bodyBytes.length);
        // 将写指针归位
        byteBuf.writerIndex(writerIndex);

        if (log.isDebugEnabled()) {
            log.debug("请求【{}】已经完成报文的编码。", lrpcRequest.getRequestId());
        }




    }

    /**
     * 将对象转换成字节数组
     * @param requestPayload
     * @return
     */
    private byte[] getBodyBytes(RequestPayload requestPayload){
        // 对象变成一个字节数据--->序列化的过程
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(baos);
            objectOutputStream.writeObject(requestPayload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();

    }

}

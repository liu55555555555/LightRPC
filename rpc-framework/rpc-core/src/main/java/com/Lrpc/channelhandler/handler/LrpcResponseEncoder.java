package com.Lrpc.channelhandler.handler;

import com.Lrpc.compress.CompressFactory;
import com.Lrpc.compress.Compressor;
import com.Lrpc.serialize.Serialize;
import com.Lrpc.serialize.SerializerFactory;
import com.Lrpc.transport.message.LrpcResponse;
import com.Lrpc.transport.message.MessageFormatConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;


/**
 * 自定义协议编码器
 * <p>
 * <pre>
 *   0    1    2    3    4    5    6    7    8    9    10   11   12   13   14   15   16   17   18   19   20   21   22
 *   +----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+----+
 *   |    magic          |ver |head  len|    full length    |code  ser|comp|              RequestId                |
 *   +-----+-----+-------+----+----+----+----+-----------+----- ---+--------+----+----+----+----+----+----+---+---+
 *   |                                                                                                             |
 *   |                                         body                                                                |
 *   |                                                                                                             |
 *   +--------------------------------------------------------------------------------------------------------+---+
 * </pre>
 *
 * 4B magic(魔数)   --->yrpc.getBytes()
 * 1B version(版本)   ----> 1
 * 2B header length 首部的长度
 * 4B full length 报文总长度
 * 1B serialize
 * 1B compress
 * 1B code
 * 8B requestId
 *
 * body
 */
@Slf4j
public class LrpcResponseEncoder extends MessageToByteEncoder<LrpcResponse> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, LrpcResponse lrpcResponse, ByteBuf byteBuf) throws Exception {
        // 4个字节的魔术值
        byteBuf.writeBytes(MessageFormatConstant.MAGIC);            //写入4字节 → 写指针移动到4
        // 一个字节的版本号
        byteBuf.writeByte(MessageFormatConstant.VERSION);           // 写入1字节 → 写指针移动到5
        //两个字节的头部长度
        byteBuf.writeShort(MessageFormatConstant.HEADER_LENGTH);    // 写入2字节 → 写指针移动到7
        //总长度的值不知道，因为不清楚body的长度。但是我们知道总长度这个字段所占用的长度是4字节。所以利用netty的writerIndex（）方法先跳过。byteBuf.writerIndex()：这是一个写指针，拿到当前写指针的位置，当之后处理完body后，知道了处理后的body的大小，我们在返回来这里填入数据
        byteBuf.writerIndex(byteBuf.writerIndex() + MessageFormatConstant.FULL_FIELD_LENGTH);//操作做了什么：byteBuf.writerIndex()不传参：获取写指针当前的位置：7,7+4==11，最外层的方法：writerIndex(11)，则将指针指向11的位置，相当于跳过了4个字节。含义：为长度字段预留4字节空间，但不写入实际数据
        // 3个类型
        byteBuf.writeByte(lrpcResponse.getCode());// 1.响应码
        byteBuf.writeByte(lrpcResponse.getSerializeType());// 2.序列化类型
        byteBuf.writeByte(lrpcResponse.getCompressType());// 3.压缩类型
        // 8字节的请求id
        byteBuf.writeLong(lrpcResponse.getRequestId());


        // 序列化 写入请求体,如果是心跳检测则不写入请求体
        Serialize serialize = SerializerFactory.getByteSerialize(lrpcResponse.getSerializeType()).getSerialize();
        byte[] bodyBytes = serialize.serialize(lrpcResponse.getResponseBody());

        //  压缩
        Compressor compressor = CompressFactory.getByteCompressWrapper(lrpcResponse.getCompressType()).getCompressor();
        bodyBytes = compressor.compress(bodyBytes);

        if (bodyBytes != null) {
            byteBuf.writeBytes(bodyBytes);
        }

        int bodyLength = bodyBytes == null ? 0 : bodyBytes.length;

        // 重新处理报文的总长度（因为之前咱们直接跳过了一段，现在返回来去补充内容）
        // 先保存当前的写指针的位置
        int writerIndex = byteBuf.writerIndex();
        // 将写指针的位置移动到总长度的位置上
        byteBuf.writerIndex(MessageFormatConstant.MAGIC.length
                + MessageFormatConstant.VERSION_LENGTH + MessageFormatConstant.HEADER_FIELD_LENGTH
        );
        byteBuf.writeInt(MessageFormatConstant.HEADER_LENGTH + bodyLength);
        // 将写指针归位
        byteBuf.writerIndex(writerIndex);

        if (log.isDebugEnabled()) {
            log.debug("响应【{}】已经完成报文的编码。", lrpcResponse.getRequestId());
        }
    }

}
package com.Lrpc.ChannelHandler.handler;

import com.Lrpc.LrpcBootstrap;
import com.Lrpc.ServiceConfig;
import com.Lrpc.enumeration.RespCode;
import com.Lrpc.transport.message.LrpcRequest;
import com.Lrpc.transport.message.LrpcResponse;
import com.Lrpc.transport.message.RequestPayload;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class MethodCallHandler extends SimpleChannelInboundHandler<LrpcRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, LrpcRequest lrpcRequest) throws Exception {
        // 1.获取负载
        RequestPayload requestPayload = lrpcRequest.getRequestPayload();

        // 2.根据负载内容进行方法调用
        Object result = callTargetMethod(requestPayload);

        if(log.isDebugEnabled()){
            log.debug("请求【{}】已经完成在服务端的方法调用",lrpcRequest.getRequestId());
        }

        // 3.封装响应
        LrpcResponse lrpcResponse = LrpcResponse.builder()
                .requestId(lrpcRequest.getRequestId())
                .code(RespCode.SUCCESS.getCode())
                .compressType(lrpcRequest.getCompressType())
                .serializeType(lrpcRequest.getSerializeType())
                .responseBody(result)
                .build();


        // 4.返回结果
        channelHandlerContext.channel().writeAndFlush(lrpcResponse);

        if(log.isDebugEnabled()){
            log.debug("请求【{}】已经将调用的方法结果返回",lrpcRequest.getRequestId());
        }


    }

    private Object callTargetMethod(RequestPayload requestPayload) {
        String interfaceName = requestPayload.getInterfaceName();
        String methodName = requestPayload.getMethodName();
        Class<?>[] parametersType = requestPayload.getParametersType();
        Object[] parametersValue = requestPayload.getParametersValue();

        // 寻找匹配的暴露出去的具体实现
        ServiceConfig<?> serviceConfig = LrpcBootstrap.SERVERS_LIST.get(interfaceName);
        Object refImpl = serviceConfig.getRef();

        // 通过反射调用 1、获取方法 2、执行invoke方法
        Class<?> refImplClass = refImpl.getClass();
        Method method ;
        Object ret;
        try {
            method = refImplClass.getMethod(methodName, parametersType);
            ret = method.invoke(refImpl, parametersValue);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            log.error("调用服务【{}】的方法【{}】时发生了异常",interfaceName,methodName, e);
            throw new RuntimeException(e);
        }
        return ret;
    }
}

package com.Lrpc.enumeration;

public enum RespCode {
    SUCCESS((byte)1, "成功"),
    FAIL((byte)2, "失败");

    private byte code;
    private String desc;

    RespCode(byte code,String desc) {
        this.desc = desc;
        this.code = code;
    }
}

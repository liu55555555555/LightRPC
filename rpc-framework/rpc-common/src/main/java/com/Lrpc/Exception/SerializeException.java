package com.Lrpc.Exception;

public class SerializeException extends RuntimeException {
    public SerializeException(String message) {
        super(message);
    }

    public SerializeException(Throwable cause) {
        super(cause);
    }
}

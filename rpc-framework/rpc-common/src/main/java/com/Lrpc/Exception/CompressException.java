package com.Lrpc.Exception;

public class CompressException extends RuntimeException {
    public CompressException(String message) {
        super(message);
    }

    public CompressException(Throwable cause) {
        super(cause);
    }
}

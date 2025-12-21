package com.Lrpc.Exception;

public class LoadBalancerException extends RuntimeException {
    public LoadBalancerException(String message) {
        super(message);
    }

    public LoadBalancerException() {
        super();
    }
}

package com.dingco.dispatch.sender;

public class PoisonMessageException extends RuntimeException {
    public PoisonMessageException(String message) {
        super(message);
    }
}

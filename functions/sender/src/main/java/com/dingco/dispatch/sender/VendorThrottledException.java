package com.dingco.dispatch.sender;

/** 발송사가 429(초당 처리 한도 초과)로 거절했다. 일시적 실패라 나중에 다시 받는다. */
public class VendorThrottledException extends RuntimeException {
    public VendorThrottledException(String message) {
        super(message);
    }
}

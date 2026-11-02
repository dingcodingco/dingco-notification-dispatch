package com.dingco.dispatch.sender;

/** 발송사가 요청을 처리했는지 알 수 없는 상태(응답 타임아웃). */
public class VendorTimeoutException extends RuntimeException {
    public VendorTimeoutException(String message) {
        super(message);
    }
}

package com.dingco.dispatch.sender;

/** 발송사 호출 경계. */
@FunctionalInterface
public interface VendorClient {
    /** idempotencyKey 를 발송사에 함께 보낸다(발송사 측 멱등 키). */
    void send(SendRequest request);

    /** 발송사에 이 키로 이미 접수된 발송이 있는지 조회한다(결과 조회). 조회를 지원하지 않는 발송사는 false. */
    default boolean alreadyAccepted(String idempotencyKey) {
        return false;
    }
}

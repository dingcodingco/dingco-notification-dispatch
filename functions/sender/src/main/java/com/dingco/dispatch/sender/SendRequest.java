package com.dingco.dispatch.sender;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 메시지 v1 NotificationSendRequested 최소 필드. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SendRequest(
        String campaignId,
        String tenantId,
        String recipientId,
        String channel,
        String payloadRef,
        String idempotencyKey,
        String traceId) {

    void validate() {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new PoisonMessageException("idempotencyKey 없음");
        }
        if (payloadRef == null || !payloadRef.startsWith("s3://")) {
            throw new PoisonMessageException("payloadRef 형식 오류: " + payloadRef);
        }
    }
}

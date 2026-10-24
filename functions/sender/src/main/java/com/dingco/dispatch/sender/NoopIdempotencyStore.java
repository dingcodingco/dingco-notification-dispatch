package com.dingco.dispatch.sender;

/** 6-1 재현용: 멱등 처리를 하지 않는다. */
public class NoopIdempotencyStore implements IdempotencyStore {
    @Override
    public Claim claim(String idempotencyKey, String traceId, String owner) {
        return Claim.CLAIMED;
    }

    @Override
    public void complete(String idempotencyKey, String owner) {
    }

    @Override
    public void release(String idempotencyKey, String owner) {
    }
}

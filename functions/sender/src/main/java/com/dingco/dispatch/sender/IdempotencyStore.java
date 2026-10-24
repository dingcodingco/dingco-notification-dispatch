package com.dingco.dispatch.sender;

/** 같은 idempotencyKey 의 발송을 한 번만 통과시키는 저장소. */
public interface IdempotencyStore {

    enum Claim {
        /** 이 호출이 키를 선점했다. 발송해도 된다. */
        CLAIMED,
        /** 이미 발송이 끝난 키다. 건너뛴다. */
        ALREADY_COMPLETED,
        /** 다른 처리가 선점 중이고 선점이 아직 유효하다. 지금은 보내지 않고 나중에 다시 받는다. */
        IN_PROGRESS_ELSEWHERE
    }

    /** owner 는 이 처리 시도를 구분하는 값(시도마다 새로 만든다). */
    Claim claim(String idempotencyKey, String traceId, String owner);

    /** 발송이 끝났음을 기록한다. 자기가 선점한 키만 바꾼다. */
    void complete(String idempotencyKey, String owner);

    /** 발송이 실패했을 때 선점을 풀어 재시도가 다시 들어올 수 있게 한다. 자기가 선점한 키만 지운다. */
    void release(String idempotencyKey, String owner);

    @SuppressWarnings("deprecation")
    static IdempotencyStore of(String mode, software.amazon.awssdk.services.dynamodb.DynamoDbClient ddb, String table) {
        return switch (mode) {
            case "off" -> new NoopIdempotencyStore();
            case "check-then-put" -> new CheckThenPutIdempotencyStore(ddb, table);
            case "conditional" -> new ConditionalWriteIdempotencyStore(ddb, table, ConditionalWriteIdempotencyStore.leaseFromEnv());
            default -> throw new IllegalArgumentException("알 수 없는 IDEMPOTENCY_MODE: " + mode);
        };
    }
}

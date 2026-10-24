package com.dingco.dispatch.sender;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * 조건부 쓰기 + 상태(IN_PROGRESS → COMPLETED) + 선점 만료.
 *
 * 선점: 키가 없거나, IN_PROGRESS 인데 선점 만료 시각(leaseUntil)이 지났을 때만 쓴다(한 번의 조건부 쓰기).
 * 완료: 발송이 끝나면 COMPLETED 로 바꾼다. 자기가 선점한 키(owner 일치)만 바꾼다.
 * 해제: 발송이 실패하면 자기 선점만 지운다.
 * 선점 뒤 함수가 중단되면 키가 IN_PROGRESS 로 남는다. 선점 만료가 지나면 다음 재전달이 다시 선점할 수 있다.
 */
public class ConditionalWriteIdempotencyStore implements IdempotencyStore {
    /** 키 보관 기간. SQS 메시지 보존 기간(기본 4일)보다 길게 잡는다. */
    static final Duration RETENTION = Duration.ofDays(7);
    /** 선점 유효 시간 기본값. 함수 제한 시간(15초)보다 길어야 살아 있는 처리의 선점을 빼앗지 않는다. */
    static final Duration DEFAULT_LEASE = Duration.ofSeconds(30);

    private final DynamoDbClient ddb;
    private final String table;
    private final Duration lease;
    private final Clock clock;

    public ConditionalWriteIdempotencyStore(DynamoDbClient ddb, String table, Duration lease) {
        this(ddb, table, lease, Clock.systemUTC());
    }

    ConditionalWriteIdempotencyStore(DynamoDbClient ddb, String table, Duration lease, Clock clock) {
        this.ddb = ddb;
        this.table = table;
        this.lease = lease;
        this.clock = clock;
    }

    static Duration leaseFromEnv() {
        String v = System.getenv("IDEMPOTENCY_LEASE_SECONDS");
        return v == null || v.isBlank() ? DEFAULT_LEASE : Duration.ofSeconds(Long.parseLong(v));
    }

    @Override
    public Claim claim(String idempotencyKey, String traceId, String owner) {
        Instant now = clock.instant();
        try {
            ddb.putItem(b -> b.tableName(table)
                    .item(Map.of(
                            "idempotencyKey", AttributeValue.fromS(idempotencyKey),
                            "status", AttributeValue.fromS("IN_PROGRESS"),
                            "owner", AttributeValue.fromS(owner),
                            "leaseUntil", AttributeValue.fromN(Long.toString(now.plus(lease).toEpochMilli())),
                            "traceId", AttributeValue.fromS(traceId == null ? "-" : traceId),
                            "expiresAt", AttributeValue.fromN(Long.toString(now.plus(RETENTION).getEpochSecond()))))
                    .conditionExpression("attribute_not_exists(idempotencyKey) OR (#s = :inprogress AND leaseUntil < :now)")
                    .expressionAttributeNames(Map.of("#s", "status"))
                    .expressionAttributeValues(Map.of(
                            ":inprogress", AttributeValue.fromS("IN_PROGRESS"),
                            ":now", AttributeValue.fromN(Long.toString(now.toEpochMilli())))));
            return Claim.CLAIMED;
        } catch (ConditionalCheckFailedException e) {
            var item = ddb.getItem(b -> b.tableName(table)
                    .key(Map.of("idempotencyKey", AttributeValue.fromS(idempotencyKey)))
                    .consistentRead(true)).item();
            return "COMPLETED".equals(item.getOrDefault("status", AttributeValue.fromS("")).s())
                    ? Claim.ALREADY_COMPLETED
                    : Claim.IN_PROGRESS_ELSEWHERE;
        }
    }

    @Override
    public void complete(String idempotencyKey, String owner) {
        ddb.updateItem(b -> b.tableName(table)
                .key(Map.of("idempotencyKey", AttributeValue.fromS(idempotencyKey)))
                .updateExpression("SET #s = :completed, completedAt = :at REMOVE leaseUntil")
                .conditionExpression("#o = :owner")
                .expressionAttributeNames(Map.of("#s", "status", "#o", "owner"))
                .expressionAttributeValues(Map.of(
                        ":completed", AttributeValue.fromS("COMPLETED"),
                        ":owner", AttributeValue.fromS(owner),
                        ":at", AttributeValue.fromS(clock.instant().toString()))));
    }

    @Override
    public void release(String idempotencyKey, String owner) {
        try {
            ddb.deleteItem(b -> b.tableName(table)
                    .key(Map.of("idempotencyKey", AttributeValue.fromS(idempotencyKey)))
                    .conditionExpression("#o = :owner AND #s = :inprogress")
                    .expressionAttributeNames(Map.of("#s", "status", "#o", "owner"))
                    .expressionAttributeValues(Map.of(
                            ":owner", AttributeValue.fromS(owner),
                            ":inprogress", AttributeValue.fromS("IN_PROGRESS"))));
        } catch (ConditionalCheckFailedException ignored) {
            // 선점이 만료돼 다른 처리가 가져갔다. 남의 선점은 지우지 않는다.
        }
    }
}

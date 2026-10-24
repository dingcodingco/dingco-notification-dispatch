package com.dingco.dispatch.sender;

import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 비교용으로 준비한 구현(6-3). Claude Code 가 만든 구현과 나란히 같은 테스트로 돌려 본다.
 * 조회(GetItem)와 저장(PutItem)이 분리돼 있어, 두 소비자가 동시에 조회하면 둘 다 "없음"을 보고 둘 다 발송한다.
 */
@Deprecated
public class CheckThenPutIdempotencyStore implements IdempotencyStore {
    private final DynamoDbClient ddb;
    private final String table;

    public CheckThenPutIdempotencyStore(DynamoDbClient ddb, String table) {
        this.ddb = ddb;
        this.table = table;
    }

    @Override
    public Claim claim(String idempotencyKey, String traceId, String owner) {
        var existing = ddb.getItem(b -> b.tableName(table)
                .key(Map.of("idempotencyKey", AttributeValue.fromS(idempotencyKey)))
                .consistentRead(true));
        if (existing.hasItem() && !existing.item().isEmpty()) {
            return Claim.ALREADY_COMPLETED;
        }
        ddb.putItem(b -> b.tableName(table).item(Map.of(
                "idempotencyKey", AttributeValue.fromS(idempotencyKey),
                "traceId", AttributeValue.fromS(traceId == null ? "-" : traceId))));
        return Claim.CLAIMED;
    }

    @Override
    public void complete(String idempotencyKey, String owner) {
    }

    @Override
    public void release(String idempotencyKey, String owner) {
        ddb.deleteItem(b -> b.tableName(table)
                .key(Map.of("idempotencyKey", AttributeValue.fromS(idempotencyKey))));
    }
}

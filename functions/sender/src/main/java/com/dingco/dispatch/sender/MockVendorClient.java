package com.dingco.dispatch.sender;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * 모의 발송사. 실제로 보내지 않고, 발송사가 받은 요청을 결과 원장(send-results)에 한 줄씩 남긴다.
 * 원장의 행 수 = 발송사가 받은 발송 요청 수. 중복 발송 여부는 이 값으로 판정한다.
 * 결과 조회는 원장의 idempotencyKey 보조 인덱스(by-idempotency-key)로 한다.
 */
public class MockVendorClient implements VendorClient {
    private final DynamoDbClient ddb;
    private final String ledgerTable;

    public MockVendorClient(DynamoDbClient ddb, String ledgerTable) {
        this.ddb = ddb;
        this.ledgerTable = ledgerTable;
    }

    @Override
    public void send(SendRequest r) {
        ddb.putItem(b -> b.tableName(ledgerTable).item(Map.of(
                "sendId", AttributeValue.fromS(UUID.randomUUID().toString()),
                "idempotencyKey", AttributeValue.fromS(r.idempotencyKey()),
                "campaignId", AttributeValue.fromS(String.valueOf(r.campaignId())),
                "tenantId", AttributeValue.fromS(String.valueOf(r.tenantId())),
                "recipientId", AttributeValue.fromS(String.valueOf(r.recipientId())),
                "traceId", AttributeValue.fromS(String.valueOf(r.traceId())),
                "sentAt", AttributeValue.fromS(Instant.now().toString()))));
    }

    @Override
    public boolean alreadyAccepted(String idempotencyKey) {
        return ddb.query(b -> b.tableName(ledgerTable).indexName("by-idempotency-key")
                .keyConditionExpression("idempotencyKey = :k")
                .expressionAttributeValues(Map.of(":k", AttributeValue.fromS(idempotencyKey)))
                .limit(1)).count() > 0;
    }
}

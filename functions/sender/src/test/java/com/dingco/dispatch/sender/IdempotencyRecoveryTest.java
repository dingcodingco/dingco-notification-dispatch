package com.dingco.dispatch.sender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * 조건부 쓰기만으로는 풀리지 않는 두 경우.
 *  1) 선점 뒤 함수가 중단됨 → 키가 IN_PROGRESS 로 남는다. 선점 만료 뒤 다시 선점해 보낸다.
 *  2) 발송사는 처리했는데 응답이 타임아웃 → 재시도가 다시 보낸다. 결과 조회로 막는다.
 * 실행 전제: LocalStack 4.9 가 localhost:4566 에 떠 있다.
 */
class IdempotencyRecoveryTest {
    static final String TABLE = "test-idempotency-recovery";
    static DynamoDbClient ddb;

    @BeforeAll
    static void createTable() {
        ddb = DynamoDbClient.builder()
                .endpointOverride(URI.create(System.getProperty("lab.endpoint", "http://localhost:4566")))
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .httpClient(UrlConnectionHttpClient.create())
                .build();
        if (!ddb.listTables().tableNames().contains(TABLE)) {
            ddb.createTable(b -> b.tableName(TABLE).billingMode(BillingMode.PAY_PER_REQUEST)
                    .attributeDefinitions(a -> a.attributeName("idempotencyKey").attributeType(ScalarAttributeType.S))
                    .keySchema(k -> k.attributeName("idempotencyKey").keyType(KeyType.HASH)));
            ddb.waiter().waitUntilTableExists(b -> b.tableName(TABLE));
        }
    }

    @AfterAll
    static void dropTable() {
        ddb.deleteTable(b -> b.tableName(TABLE));
    }

    static SendRequest request(String tag) {
        String id = tag + "-" + UUID.randomUUID().toString().substring(0, 8);
        return new SendRequest("cmp-rec", "tenant-a", id, "SMS", "s3://lab-payloads/cmp-rec/" + id + ".json",
                "tenant-a:cmp-rec:" + id + ":SMS", "trace-" + id);
    }

    /** 발송사 흉내: 받은 키를 기억하고(발송사 측 멱등 키 저장), 첫 호출만 응답 타임아웃을 낸다. */
    static class FlakyVendor implements VendorClient {
        final Set<String> accepted = ConcurrentHashMap.newKeySet();
        final AtomicInteger sends = new AtomicInteger();
        boolean timeoutOnce = true;

        @Override
        public void send(SendRequest r) {
            sends.incrementAndGet();
            accepted.add(r.idempotencyKey());
            if (timeoutOnce) {
                timeoutOnce = false;
                throw new VendorTimeoutException("발송사 응답 타임아웃(처리 여부 모름)");
            }
        }

        @Override
        public boolean alreadyAccepted(String key) {
            return accepted.contains(key);
        }
    }

    @Test
    void 선점_뒤_중단되면_선점_만료_뒤에_다시_보낸다() throws Exception {
        var store = new ConditionalWriteIdempotencyStore(ddb, TABLE, Duration.ofSeconds(2));
        AtomicInteger sends = new AtomicInteger();
        VendorClient vendor = new VendorClient() {
            public void send(SendRequest r) { sends.incrementAndGet(); }
            public boolean alreadyAccepted(String k) { return false; }
        };
        var service = new SenderService(store, vendor, ref -> { }, true);
        SendRequest req = request("crash");

        // 앞선 시도: 선점만 하고 함수가 중단됐다(complete 도 release 도 못 함)
        assertEquals(IdempotencyStore.Claim.CLAIMED, store.claim(req.idempotencyKey(), req.traceId(), "crashed-attempt"));
        System.out.println("[crash] 앞선 시도가 선점 후 중단됨");

        // 곧바로 재전달: 선점이 아직 유효하므로 보내지 않고 나중에 다시 받는다
        assertThrows(InProgressElsewhereException.class, () -> service.process(req));
        System.out.println("[crash] 즉시 재전달 → IN_PROGRESS_ELSEWHERE, send count=" + sends.get());

        Thread.sleep(2500);
        System.out.println("[crash] 선점 만료(2초) 뒤 재전달 → " + service.process(req) + ", send count=" + sends.get());
        System.out.println("[crash] 한 번 더 재전달 → " + service.process(req) + ", send count=" + sends.get());
        assertEquals(1, sends.get(), "crash recovery send count");
    }

    @Test
    void 응답_타임아웃_뒤_재시도_결과_조회가_없으면_두_번_보낸다() {
        var store = new ConditionalWriteIdempotencyStore(ddb, TABLE, Duration.ofSeconds(30));
        var vendor = new FlakyVendor();
        var service = new SenderService(store, vendor, ref -> { }, false);
        SendRequest req = request("timeout-noquery");

        assertThrows(VendorTimeoutException.class, () -> service.process(req));
        System.out.println("[timeout/결과 조회 없음] 1차 시도 → 타임아웃, 발송사 수신=" + vendor.sends.get());
        System.out.println("[timeout/결과 조회 없음] 재시도 → " + service.process(req) + ", 발송사 수신=" + vendor.sends.get());
        assertEquals(2, vendor.sends.get(), "결과 조회 없이 재시도하면 발송사가 두 번 받는다");
    }

    @Test
    void 응답_타임아웃_뒤_재시도_결과_조회로_한_번만_보낸다() {
        var store = new ConditionalWriteIdempotencyStore(ddb, TABLE, Duration.ofSeconds(30));
        var vendor = new FlakyVendor();
        var service = new SenderService(store, vendor, ref -> { }, true);
        SendRequest req = request("timeout-query");

        assertThrows(VendorTimeoutException.class, () -> service.process(req));
        System.out.println("[timeout/결과 조회] 1차 시도 → 타임아웃, 발송사 수신=" + vendor.sends.get());
        System.out.println("[timeout/결과 조회] 재시도 → " + service.process(req) + ", 발송사 수신=" + vendor.sends.get());
        assertEquals(1, vendor.sends.get(), "결과 조회로 복구하면 발송사는 한 번만 받는다");
    }
}

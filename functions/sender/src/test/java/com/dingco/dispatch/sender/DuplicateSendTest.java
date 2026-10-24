package com.dingco.dispatch.sender;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * 같은 idempotencyKey 메시지 두 개가 들어올 때 발송사 호출 수를 센다.
 *  - 차례로: 첫 처리가 끝난 뒤 두 번째가 온다(순차 중복)
 *  - 동시에: barrier 로 두 스레드의 시작 시점을 맞춘다(동시 경합)
 * 두 경우는 다른 문제다. 순차 테스트만 통과해서는 동시 경합이 검증되지 않는다.
 *
 * 실행 전제: LocalStack 4.9 가 localhost:4566 에 떠 있다 (scripts/local/up.sh).
 * 구현 선택: -Didempotency.mode=conditional (기본) | check-then-put (6-3에서 reject 한 구현)
 */
@SuppressWarnings({"unchecked", "deprecation"})
class DuplicateSendTest {
    static final String TABLE = "test-idempotency-keys";
    static final int ROUNDS = 5;
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
            ddb.createTable(b -> b.tableName(TABLE)
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .attributeDefinitions(a -> a.attributeName("idempotencyKey").attributeType(ScalarAttributeType.S))
                    .keySchema(k -> k.attributeName("idempotencyKey").keyType(KeyType.HASH)));
            ddb.waiter().waitUntilTableExists(b -> b.tableName(TABLE));
        }
    }

    @AfterAll
    static void dropTable() {
        ddb.deleteTable(b -> b.tableName(TABLE));
    }

    static SendRequest request(String tag, int round) {
        return new SendRequest("cmp-" + tag, "tenant-a", "rcp-" + round, "SMS",
                "s3://lab-payloads/cmp-" + tag + "/rcp-" + round + ".json",
                "tenant-a:cmp-" + tag + ":rcp-" + round + ":SMS-" + UUID.randomUUID(), "trace-" + tag + "-" + round);
    }

    /** 늦게 온 쪽은 "다른 처리가 선점 중"으로 실패하고 나중에 다시 받는다. 발송하지 않았으므로 정상 결과다. */
    static String outcome(SenderService service, SendRequest req) {
        try {
            return service.process(req).name();
        } catch (InProgressElsewhereException e) {
            return "IN_PROGRESS_ELSEWHERE";
        }
    }

    @Test
    void 같은_키_두_건이_차례로_와도_발송은_한_번() {
        String mode = System.getProperty("idempotency.mode", "conditional");
        IdempotencyStore store = IdempotencyStore.of(mode, ddb, TABLE);
        AtomicInteger sends = new AtomicInteger();
        SenderService service = new SenderService(store, r -> sends.incrementAndGet());
        SendRequest req = request("seq", 1);
        service.process(req);
        service.process(req);
        System.out.println("[sequential] idempotency.mode=" + mode + " send count=" + sends.get());
        assertEquals(1, sends.get(), "sequential send count");
    }

    @Test
    void 같은_키_두_건이_동시에_와도_발송은_한_번() throws Exception {
        String mode = System.getProperty("idempotency.mode", "conditional");
        IdempotencyStore store = IdempotencyStore.of(mode, ddb, TABLE);
        System.out.println("[concurrent] idempotency.mode=" + mode + " rounds=" + ROUNDS);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 1; round <= ROUNDS; round++) {
                AtomicInteger sends = new AtomicInteger();
                SenderService service = new SenderService(store, r -> sends.incrementAndGet());
                SendRequest req = request("race", round);

                CyclicBarrier barrier = new CyclicBarrier(2);
                Future<String> a = pool.submit(() -> { barrier.await(); return outcome(service, req); });
                Future<String> b = pool.submit(() -> { barrier.await(); return outcome(service, req); });
                System.out.println("[concurrent] round " + round + " outcomes=" + a.get(10, TimeUnit.SECONDS) + "," + b.get(10, TimeUnit.SECONDS));

                System.out.println("[concurrent] round " + round + " send count=" + sends.get());
                assertEquals(1, sends.get(), "concurrent round " + round + " send count");
            }
        } finally {
            pool.shutdownNow();
        }
    }
}

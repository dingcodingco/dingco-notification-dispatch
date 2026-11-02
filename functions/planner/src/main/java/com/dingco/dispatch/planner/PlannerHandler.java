package com.dingco.dispatch.planner;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

/**
 * NotificationDispatchPlanned 1건을 받아 수신자 파일(S3 CSV)을 읽고,
 * 수신자마다 NotificationSendRequested 메시지를 만들어 발송 큐에 10건씩 넣는다(fan-out).
 * 본문은 메시지에 싣지 않고 payloadRef(S3 위치)만 싣는다.
 * FAIR_QUEUE=true 면 MessageGroupId 에 tenantId 를 넣는다(SQS Standard 큐의 fair queue).
 */
public class PlannerHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final S3Client s3;
    private final SqsClient sqs;
    private final DynamoDbClient ddb;
    private final String sendQueueUrl;
    private final String recipientsBucket = env("RECIPIENTS_BUCKET", "lab-recipients");
    private final String campaignsTable = env("CAMPAIGNS_TABLE", "campaigns");
    private final boolean fairQueue = Boolean.parseBoolean(env("FAIR_QUEUE", "false"));

    public PlannerHandler() {
        String endpoint = System.getenv("AWS_ENDPOINT_URL");
        Region region = Region.of(env("AWS_REGION", "ap-northeast-2"));
        var s3b = S3Client.builder().httpClient(UrlConnectionHttpClient.create()).region(region);
        var sqsb = SqsClient.builder().httpClient(UrlConnectionHttpClient.create()).region(region);
        var ddbb = DynamoDbClient.builder().httpClient(UrlConnectionHttpClient.create()).region(region);
        if (endpoint != null && !endpoint.isBlank()) {
            s3b.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
            sqsb.endpointOverride(URI.create(endpoint));
            ddbb.endpointOverride(URI.create(endpoint));
        }
        s3 = s3b.build(); sqs = sqsb.build(); ddb = ddbb.build();
        sendQueueUrl = sqs.getQueueUrl(b -> b.queueName(env("SEND_QUEUE", "notify-send-requested"))).queueUrl();
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();
        for (SQSEvent.SQSMessage m : event.getRecords()) {
            try {
                plan(JSON.readTree(m.getBody()));
            } catch (Exception e) {
                System.out.println("[planner] messageId=" + m.getMessageId() + " result=FAILED reason=" + e.getMessage());
                failures.add(new SQSBatchResponse.BatchItemFailure(m.getMessageId()));
            }
        }
        return new SQSBatchResponse(failures);
    }

    void plan(JsonNode p) throws Exception {
        long start = System.currentTimeMillis();
        String campaignId = p.get("campaignId").asText(), tenantId = p.get("tenantId").asText(), channel = p.get("channel").asText();
        String payloadRef = p.get("payloadRef").asText(), traceId = p.get("traceId").asText(), objectKey = p.get("objectKey").asText();
        AtomicLong enqueued = new AtomicLong();
        long recipients = 0;
        List<Future<?>> inflight = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(16);
             var in = s3.getObject(b -> b.bucket(recipientsBucket).key(objectKey));
             var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<SendMessageBatchRequestEntry> batch = new ArrayList<>(10);
            String line;
            while ((line = reader.readLine()) != null) {
                String recipientId = line.trim();
                if (recipientId.isEmpty() || recipientId.equals("recipientId")) continue;
                recipients++;
                String body = "{\"type\":\"NotificationSendRequested\",\"version\":1,\"campaignId\":\"" + campaignId
                        + "\",\"tenantId\":\"" + tenantId + "\",\"recipientId\":\"" + recipientId + "\",\"channel\":\"" + channel
                        + "\",\"payloadRef\":\"" + payloadRef + "\",\"idempotencyKey\":\"" + tenantId + ":" + campaignId + ":" + recipientId + ":" + channel
                        + "\",\"traceId\":\"" + traceId + "\"}";
                var entry = SendMessageBatchRequestEntry.builder().id("m" + batch.size()).messageBody(body);
                if (fairQueue) entry.messageGroupId(tenantId);
                batch.add(entry.build());
                if (batch.size() == 10) {
                    var toSend = batch;
                    inflight.add(pool.submit(() -> enqueued.addAndGet(sendBatch(toSend))));
                    batch = new ArrayList<>(10);
                }
            }
            if (!batch.isEmpty()) {
                var toSend = batch;
                inflight.add(pool.submit(() -> enqueued.addAndGet(sendBatch(toSend))));
            }
            for (Future<?> f : inflight) f.get();
        }
        long total = recipients;
        ddb.updateItem(b -> b.tableName(campaignsTable).key(Map.of("campaignId", AttributeValue.fromS(campaignId)))
                .updateExpression("SET #s = :s, totalRecipients = :t, enqueuedCount = :e, lastUpdatedAt = :at")
                .expressionAttributeNames(Map.of("#s", "status"))
                .expressionAttributeValues(Map.of(":s", AttributeValue.fromS("DISPATCHING"),
                        ":t", AttributeValue.fromN(Long.toString(total)), ":e", AttributeValue.fromN(Long.toString(enqueued.get())),
                        ":at", AttributeValue.fromS(Instant.now().toString()))));
        System.out.println("[planner] campaignId=" + campaignId + " recipients=" + total + " enqueued=" + enqueued.get()
                + " fairQueue=" + fairQueue + " elapsedMs=" + (System.currentTimeMillis() - start) + " traceId=" + traceId);
    }

    /** 10건 묶음 발행. 실패한 항목은 한 번 다시 보낸다. 성공 건수를 돌려준다(생산자 성공 응답 기준). */
    int sendBatch(List<SendMessageBatchRequestEntry> entries) {
        var res = sqs.sendMessageBatch(b -> b.queueUrl(sendQueueUrl).entries(entries));
        int ok = res.successful().size();
        if (!res.failed().isEmpty()) {
            var ids = res.failed().stream().map(f -> f.id()).toList();
            var retry = entries.stream().filter(e -> ids.contains(e.id())).toList();
            ok += sqs.sendMessageBatch(b -> b.queueUrl(sendQueueUrl).entries(retry)).successful().size();
        }
        return ok;
    }

    static String env(String k, String d) {
        String v = System.getenv(k);
        return v == null || v.isBlank() ? d : v;
    }
}

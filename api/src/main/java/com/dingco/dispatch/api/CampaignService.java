package com.dingco.dispatch.api;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * 생성: 캠페인 저장 + 수신자 파일 업로드용 presigned URL 발급(별도 공개 upload API 를 두지 않는다).
 * 발송 접수: 파일 확인 → 발송 본문을 S3 에 저장(payloadRef) → 상태 QUEUED → 계획 메시지 1건 발행. 수신자 수와 상관없이 바로 응답한다.
 * 상태 조회: 캠페인 + 결과 원장(by-campaign 인덱스) 집계.
 */
@Service
public class CampaignService {
    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);
    private final LabProperties lab;
    private final DynamoDbClient ddb;
    private final S3Client s3;
    private final S3Presigner presigner;
    private final SqsClient sqs;

    public CampaignService(LabProperties lab, DynamoDbClient ddb, S3Client s3, S3Presigner presigner, SqsClient sqs) {
        this.lab = lab;
        this.ddb = ddb;
        this.s3 = s3;
        this.presigner = presigner;
        this.sqs = sqs;
    }

    public static class NotFound extends RuntimeException {
        public NotFound(String m) { super(m); }
    }

    public static class Conflict extends RuntimeException {
        public Conflict(String m) { super(m); }
    }

    public Map<String, Object> create(CampaignController.CreateCampaignRequest req) {
        String id = "cmp-" + UUID.randomUUID().toString().substring(0, 8);
        String objectKey = "recipients/" + id + ".csv";
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(lab.uploadUrlTtlMinutes()));
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("campaignId", s(id));
        item.put("tenantId", s(req.tenantId()));
        item.put("name", s(req.name()));
        item.put("channel", s(req.channel()));
        item.put("template", s(req.template()));
        item.put("status", s("DRAFT"));
        item.put("objectKey", s(objectKey));
        item.put("traceId", s(TraceIdFilter.current()));
        item.put("lastUpdatedAt", s(now.toString()));
        ddb.putItem(b -> b.tableName(lab.campaignsTable()).item(item));
        String uploadUrl = presigner.presignPutObject(p -> p.signatureDuration(Duration.ofMinutes(lab.uploadUrlTtlMinutes()))
                .putObjectRequest(o -> o.bucket(lab.recipientsBucket()).key(objectKey).contentType("text/csv"))).url().toString();
        log.info("campaign created campaignId={} tenantId={}", id, req.tenantId());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("campaignId", id);
        res.put("status", "DRAFT");
        res.put("uploadUrl", uploadUrl);
        res.put("objectKey", objectKey);
        res.put("expiresAt", expiresAt.toString());
        return res;
    }

    public Map<String, Object> dispatch(String id) {
        Map<String, AttributeValue> c = load(id);
        String objectKey = c.get("objectKey").s();
        try {
            s3.headObject(b -> b.bucket(lab.recipientsBucket()).key(objectKey));
        } catch (NoSuchKeyException e) {
            throw new Conflict("수신자 파일이 아직 업로드되지 않았습니다: " + objectKey);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) throw new Conflict("수신자 파일이 아직 업로드되지 않았습니다: " + objectKey);
            throw e;
        }
        String payloadKey = id + "/body.json";
        s3.putObject(b -> b.bucket(lab.payloadsBucket()).key(payloadKey).contentType("application/json"),
                RequestBody.fromString("{\"template\":" + json(c.get("template").s()) + "}"));
        String dispatchJobId = "job-" + UUID.randomUUID().toString().substring(0, 8);
        String traceId = TraceIdFilter.current();
        try {
            ddb.updateItem(b -> b.tableName(lab.campaignsTable())
                    .key(Map.of("campaignId", s(id)))
                    .updateExpression("SET #s = :queued, dispatchJobId = :job, dispatchTraceId = :trace, lastUpdatedAt = :at")
                    .conditionExpression("#s = :draft")
                    .expressionAttributeNames(Map.of("#s", "status"))
                    .expressionAttributeValues(Map.of(":queued", s("QUEUED"), ":draft", s("DRAFT"), ":job", s(dispatchJobId),
                            ":trace", s(traceId), ":at", s(Instant.now().toString()))));
        } catch (ConditionalCheckFailedException e) {
            throw new Conflict("이미 발송을 시작한 캠페인입니다: " + id);
        }
        String message = "{\"type\":\"NotificationDispatchPlanned\",\"version\":1"
                + ",\"campaignId\":" + json(id) + ",\"tenantId\":" + json(c.get("tenantId").s())
                + ",\"channel\":" + json(c.get("channel").s()) + ",\"objectKey\":" + json(objectKey)
                + ",\"payloadRef\":" + json("s3://" + lab.payloadsBucket() + "/" + payloadKey)
                + ",\"dispatchJobId\":" + json(dispatchJobId) + ",\"traceId\":" + json(traceId) + "}";
        String queueUrl = sqs.getQueueUrl(b -> b.queueName(lab.plannedQueue())).queueUrl();
        sqs.sendMessage(b -> b.queueUrl(queueUrl).messageBody(message));
        log.info("dispatch queued campaignId={} dispatchJobId={}", id, dispatchJobId);
        return Map.of("dispatchJobId", dispatchJobId, "status", "QUEUED");
    }

    public Map<String, Object> status(String id) {
        Map<String, AttributeValue> c = load(id);
        long total = c.containsKey("totalRecipients") ? Long.parseLong(c.get("totalRecipients").n()) : 0;
        long sent = countSent(id);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("campaignId", id);
        res.put("status", total > 0 && sent >= total ? "COMPLETED" : c.get("status").s());
        res.put("totalRecipients", total);
        res.put("pendingCount", Math.max(0, total - sent));
        res.put("sentCount", sent);
        res.put("failedCount", 0);
        res.put("dlqCount", dlqApproximate());
        res.put("lastUpdatedAt", c.get("lastUpdatedAt").s());
        return res;
    }

    private long countSent(String id) {
        long count = 0;
        Map<String, AttributeValue> start = null;
        do {
            var req = QueryRequest.builder().tableName(lab.ledgerTable()).indexName("by-campaign")
                    .keyConditionExpression("campaignId = :c").expressionAttributeValues(Map.of(":c", s(id)))
                    .select("COUNT").exclusiveStartKey(start).build();
            var res = ddb.query(req);
            count += res.count();
            start = res.hasLastEvaluatedKey() && !res.lastEvaluatedKey().isEmpty() ? res.lastEvaluatedKey() : null;
        } while (start != null);
        return count;
    }

    /** DLQ 전체의 근사 메시지 수(캠페인별 값이 아니다). */
    private long dlqApproximate() {
        String url = sqs.getQueueUrl(b -> b.queueName(lab.sendDlq())).queueUrl();
        return Long.parseLong(sqs.getQueueAttributes(b -> b.queueUrl(url).attributeNamesWithStrings("ApproximateNumberOfMessages"))
                .attributesAsStrings().get("ApproximateNumberOfMessages"));
    }

    private Map<String, AttributeValue> load(String id) {
        var item = ddb.getItem(b -> b.tableName(lab.campaignsTable()).key(Map.of("campaignId", s(id))).consistentRead(true)).item();
        if (item == null || item.isEmpty()) throw new NotFound("캠페인이 없습니다: " + id);
        return item;
    }

    private static AttributeValue s(String v) {
        return AttributeValue.fromS(v == null ? "-" : v);
    }

    private static String json(String v) {
        return "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}

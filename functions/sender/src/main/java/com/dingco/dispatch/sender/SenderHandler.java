package com.dingco.dispatch.sender;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * SQS notify-send-requested 를 소비하는 Sender Lambda.
 *
 * REPORT_BATCH_ITEM_FAILURES=true  : 실패한 메시지 ID만 돌려준다(이벤트 소스 매핑에 ReportBatchItemFailures 가 켜져 있어야 의미가 있다).
 * REPORT_BATCH_ITEM_FAILURES=false : 한 건이라도 실패하면 예외를 던져 배치 전체를 실패시킨다.
 */
public class SenderHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final SenderService service;
    private final boolean reportBatchItemFailures;

    public SenderHandler() {
        var ddb = Clients.dynamoDb();
        String mode = Clients.env("IDEMPOTENCY_MODE", "conditional");
        var ledger = new MockVendorClient(ddb, Clients.env("LEDGER_TABLE", "send-results"));
        String vendorMode = Clients.env("VENDOR_MODE", "ledger");
        VendorClient vendor = "http".equals(vendorMode)
                ? new HttpVendorClient(Clients.env("VENDOR_URL", "http://host.docker.internal:8081"), ledger,
                        Integer.parseInt(Clients.env("VENDOR_MAX_RPS", "0")))
                : ledger;
        this.service = new SenderService(
                IdempotencyStore.of(mode, ddb, Clients.env("IDEMPOTENCY_TABLE", "idempotency-keys")),
                vendor,
                new S3PayloadResolver(Clients.s3()),
                "conditional".equals(mode));
        this.reportBatchItemFailures = Boolean.parseBoolean(Clients.env("REPORT_BATCH_ITEM_FAILURES", "true"));
        System.out.println("[sender] init IDEMPOTENCY_MODE=" + mode + " REPORT_BATCH_ITEM_FAILURES=" + reportBatchItemFailures);
        if ("http".equals(vendorMode)) {
            System.out.println("[sender-config] VENDOR_MODE=http VENDOR_MAX_RPS=" + Clients.env("VENDOR_MAX_RPS", "0"));
        }
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();
        System.out.println("[sender] batch size=" + event.getRecords().size());
        for (SQSEvent.SQSMessage m : event.getRecords()) {
            String receiveCount = m.getAttributes() == null ? "?" : m.getAttributes().get("ApproximateReceiveCount");
            String key = "?";
            SendRequest req = null;
            String result;
            try {
                req = JSON.readValue(m.getBody(), SendRequest.class);
                key = req.idempotencyKey();
                SenderService.Outcome outcome = service.process(req);
                result = outcome.name();
                log(m.getMessageId(), key, receiveCount, result, null);
            } catch (Exception e) {
                result = e instanceof VendorThrottledException ? "THROTTLED" : "FAILED";
                log(m.getMessageId(), key, receiveCount, "FAILED", e.getMessage());
                failures.add(new SQSBatchResponse.BatchItemFailure(m.getMessageId()));
            }
            trace(m, req, result);
        }
        if (!failures.isEmpty() && !reportBatchItemFailures) {
            throw new IllegalStateException("배치 중 " + failures.size() + "건 실패 — 배치 전체를 실패 처리");
        }
        return new SQSBatchResponse(failures);
    }

    /** 대기시간·traceId 줄. waitMs = 처리 시각 - SQS SentTimestamp(재전달이면 최초 발행부터). */
    private static void trace(SQSEvent.SQSMessage m, SendRequest req, String result) {
        String sent = m.getAttributes() == null ? null : m.getAttributes().get("SentTimestamp");
        long waitMs = sent == null ? -1 : System.currentTimeMillis() - Long.parseLong(sent);
        System.out.println("[sender-trace] messageId=" + m.getMessageId()
                + " tenantId=" + (req == null ? "?" : req.tenantId())
                + " campaignId=" + (req == null ? "?" : req.campaignId())
                + " waitMs=" + waitMs
                + " result=" + result
                + " traceId=" + (req == null ? "?" : req.traceId()));
    }

    private static void log(String messageId, String key, String receiveCount, String result, String reason) {
        System.out.println("[sender] messageId=" + messageId
                + " idempotencyKey=" + key
                + " receiveCount=" + receiveCount
                + " result=" + result
                + (reason == null ? "" : " reason=" + reason));
    }
}

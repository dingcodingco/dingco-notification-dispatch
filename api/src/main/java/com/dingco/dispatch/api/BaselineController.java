package com.dingco.dispatch.api;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Section 2 baseline 전용(공개 계약 밖). 요청 스레드 안에서 수신자 수만큼 발송사를 차례로 호출한다.
 * 수신자가 늘어날수록 응답이 길어지는 구조를 재현하기 위한 코드이며, 이후 섹션에서 비동기 경로로 대체한다.
 */
@RestController
public class BaselineController {
    private static final Logger log = LoggerFactory.getLogger(BaselineController.class);
    private final RestClient vendor;

    public BaselineController(LabProperties lab) {
        this.vendor = RestClient.builder().baseUrl(lab.vendorUrl()).build();
    }

    public record SyncDispatchRequest(String campaignId, int recipients) { }

    @PostMapping("/baseline/sync-dispatch")
    public Map<String, Object> syncDispatch(@RequestBody SyncDispatchRequest req) {
        long start = System.currentTimeMillis();
        int sent = 0;
        for (int i = 1; i <= req.recipients(); i++) {
            vendor.post().uri("/send").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("campaignId", req.campaignId(), "tenantId", "tenant-a",
                            "recipientId", "rcp-%05d".formatted(i), "traceId", TraceIdFilter.current()))
                    .retrieve().toBodilessEntity();
            sent++;
        }
        long elapsed = System.currentTimeMillis() - start;
        log.info("sync-dispatch campaign={} recipients={} sent={} elapsedMs={}", req.campaignId(), req.recipients(), sent, elapsed);
        return Map.of("campaignId", req.campaignId(), "sent", sent, "elapsedMs", elapsed);
    }
}

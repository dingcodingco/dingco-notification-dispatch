package com.dingco.dispatch.sender;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP 모의 발송사(mock-vendor)를 부르는 발송사 클라이언트. Section 4·5 에서 쓴다.
 * 200 이면 결과 원장에 한 줄을 남긴다. 429 면 VendorThrottledException(일시적 실패), 그 밖의 오류는 예외.
 * 요청에 Idempotency-Key 헤더로 멱등 키를 함께 보낸다.
 * VENDOR_MAX_RPS(인스턴스당 초당 요청 수, 0=제한 없음)를 주면 이 인스턴스에서 나가는 요청 간격을 벌린다.
 * 전체 발송 속도 ≈ 동시 실행 수 × 인스턴스당 속도. 이 값을 발송사 한도 아래로 맞추는 게 Section 5 의 역압 설계다.
 */
public class HttpVendorClient implements VendorClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final URI sendUri;
    private final MockVendorClient ledger;
    private final long minIntervalNanos;
    private long nextAllowed = System.nanoTime();

    public HttpVendorClient(String vendorUrl, MockVendorClient ledger, int maxRps) {
        this.sendUri = URI.create(vendorUrl + "/send");
        this.ledger = ledger;
        this.minIntervalNanos = maxRps > 0 ? 1_000_000_000L / maxRps : 0;
    }

    /** 인스턴스당 속도 제한: 앞 요청과의 간격이 최소 간격보다 짧으면 기다린다. */
    private synchronized void pace() {
        if (minIntervalNanos == 0) return;
        long now = System.nanoTime();
        if (now < nextAllowed) {
            try {
                Thread.sleep((nextAllowed - now) / 1_000_000, (int) ((nextAllowed - now) % 1_000_000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        nextAllowed = Math.max(now, nextAllowed) + minIntervalNanos;
    }

    @Override
    public void send(SendRequest r) {
        pace();
        String body = "{\"campaignId\":\"" + r.campaignId() + "\",\"tenantId\":\"" + r.tenantId() + "\",\"recipientId\":\""
                + r.recipientId() + "\",\"traceId\":\"" + r.traceId() + "\"}";
        HttpResponse<String> res;
        try {
            res = http.send(HttpRequest.newBuilder(sendUri).timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json").header("Idempotency-Key", r.idempotencyKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException e) {
            throw new VendorTimeoutException("발송사 호출 실패(처리 여부 모름): " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VendorTimeoutException("발송사 호출 중단");
        }
        if (res.statusCode() == 429) {
            throw new VendorThrottledException("발송사 429 Too Many Requests");
        }
        if (res.statusCode() / 100 != 2) {
            throw new IllegalStateException("발송사 오류 " + res.statusCode());
        }
        ledger.send(r);
    }

    @Override
    public boolean alreadyAccepted(String idempotencyKey) {
        return ledger.alreadyAccepted(idempotencyKey);
    }
}

package com.dingco.dispatch.mockvendor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 모의 발송사. 실제로 보내지 않고 받은 요청을 센다.
 *
 * POST /send   : 요청 1건을 받는다. 지연(MOCK_VENDOR_LATENCY_MS)만큼 기다린 뒤 200.
 *                초당 처리 한도(MOCK_VENDOR_RATE_LIMIT, 0이면 무제한)를 넘으면 429 + Retry-After: 1.
 * GET  /stats  : 받은 수·429 수·최대 초당 요청 수·테넌트별 받은 수.
 * POST /reset  : 통계를 비운다. 설정(지연·한도)은 쿼리로 바꿀 수 있다: /reset?latencyMs=20&rateLimit=50
 */
public class MockVendorServer {
    static volatile long latencyMs = envLong("MOCK_VENDOR_LATENCY_MS", 20);
    static volatile long rateLimit = envLong("MOCK_VENDOR_RATE_LIMIT", 0);

    static final AtomicLong accepted = new AtomicLong();
    static final AtomicLong throttled = new AtomicLong();
    static final Map<Long, AtomicLong> perSecond = new ConcurrentHashMap<>();
    static final Map<String, AtomicLong> byTenant = new ConcurrentHashMap<>();
    static final Pattern TENANT = Pattern.compile("\"tenantId\"\\s*:\\s*\"([^\"]+)\"");

    public static void main(String[] args) throws IOException {
        int port = (int) envLong("MOCK_VENDOR_PORT", 8081);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 512);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/send", MockVendorServer::send);
        server.createContext("/stats", MockVendorServer::stats);
        server.createContext("/reset", MockVendorServer::reset);
        server.start();
        System.out.println("[mock-vendor] port=" + port + " latencyMs=" + latencyMs + " rateLimit=" + rateLimit);
    }

    static void send(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        long second = System.currentTimeMillis() / 1000;
        long countThisSecond = perSecond.computeIfAbsent(second, k -> new AtomicLong()).incrementAndGet();
        if (rateLimit > 0 && countThisSecond > rateLimit) {
            throttled.incrementAndGet();
            ex.getResponseHeaders().add("Retry-After", "1");
            reply(ex, 429, "{\"error\":\"too many requests\"}");
            return;
        }
        sleep(latencyMs);
        accepted.incrementAndGet();
        Matcher m = TENANT.matcher(body);
        byTenant.computeIfAbsent(m.find() ? m.group(1) : "-", k -> new AtomicLong()).incrementAndGet();
        reply(ex, 200, "{\"vendorMessageId\":\"" + UUID.randomUUID() + "\"}");
    }

    static void stats(HttpExchange ex) throws IOException {
        long maxRps = perSecond.values().stream().mapToLong(AtomicLong::get).max().orElse(0);
        StringBuilder tenants = new StringBuilder();
        byTenant.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e ->
                tenants.append(tenants.isEmpty() ? "" : ",").append('"').append(e.getKey()).append("\":").append(e.getValue().get()));
        reply(ex, 200, "{\"accepted\":" + accepted.get() + ",\"throttled429\":" + throttled.get()
                + ",\"maxRequestsPerSecond\":" + maxRps + ",\"latencyMs\":" + latencyMs + ",\"rateLimit\":" + rateLimit
                + ",\"byTenant\":{" + tenants + "}}");
    }

    static void reset(HttpExchange ex) throws IOException {
        String q = ex.getRequestURI().getQuery();
        if (q != null) {
            for (String kv : q.split("&")) {
                String[] p = kv.split("=");
                if (p[0].equals("latencyMs")) latencyMs = Long.parseLong(p[1]);
                if (p[0].equals("rateLimit")) rateLimit = Long.parseLong(p[1]);
            }
        }
        accepted.set(0); throttled.set(0); perSecond.clear(); byTenant.clear();
        reply(ex, 200, "{\"reset\":true,\"latencyMs\":" + latencyMs + ",\"rateLimit\":" + rateLimit + "}");
    }

    static void reply(HttpExchange ex, int code, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    static long envLong(String k, long d) {
        String v = System.getenv(k);
        return v == null || v.isBlank() ? d : Long.parseLong(v);
    }
}

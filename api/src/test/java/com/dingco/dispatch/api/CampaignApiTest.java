package com.dingco.dispatch.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

/**
 * 공개 REST 계약 3개를 실제 LocalStack 리소스로 확인한다.
 * 실행 전제: scripts/local/up.sh + scripts/local/deploy.sh 로 스택이 떠 있고, source scripts/local/env.sh 한 셸에서 실행.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "lab.endpoint=http://localhost:4566")
class CampaignApiTest {
    @Autowired
    TestRestTemplate rest;

    @SuppressWarnings("unchecked")
    Map<String, Object> create() {
        ResponseEntity<Map> res = rest.postForEntity("/campaigns",
                Map.of("tenantId", "tenant-a", "name", "test", "channel", "SMS", "template", "hi"), Map.class);
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        return res.getBody();
    }

    @Test
    void 캠페인_생성_응답에_업로드_정보_5개가_있다() {
        Map<String, Object> body = create();
        assertThat(body).containsKeys("campaignId", "status", "uploadUrl", "objectKey", "expiresAt");
        assertThat(body.get("status")).isEqualTo("DRAFT");
        System.out.println("[api-test] created " + body.get("campaignId") + " objectKey=" + body.get("objectKey"));
    }

    @Test
    void 수신자_파일_없이_발송하면_409() {
        String id = (String) create().get("campaignId");
        var res = rest.postForEntity("/campaigns/" + id + "/dispatch", null, Map.class);
        System.out.println("[api-test] dispatch without file → " + res.getStatusCode().value() + " " + res.getBody());
        assertThat(res.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void 파일을_올리고_발송하면_202_QUEUED() throws Exception {
        Map<String, Object> body = create();
        String id = (String) body.get("campaignId");
        var put = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create((String) body.get("uploadUrl")))
                .header("Content-Type", "text/csv").PUT(HttpRequest.BodyPublishers.ofString("recipientId\nr-1\nr-2\n")).build(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(put.statusCode()).isEqualTo(200);
        var res = rest.postForEntity("/campaigns/" + id + "/dispatch", null, Map.class);
        System.out.println("[api-test] dispatch after upload → " + res.getStatusCode().value() + " " + res.getBody());
        assertThat(res.getStatusCode().value()).isEqualTo(202);
        assertThat(res.getBody()).containsEntry("status", "QUEUED");
        var again = rest.postForEntity("/campaigns/" + id + "/dispatch", null, Map.class);
        System.out.println("[api-test] dispatch twice → " + again.getStatusCode().value());
        assertThat(again.getStatusCode().value()).isEqualTo(409);
    }
}

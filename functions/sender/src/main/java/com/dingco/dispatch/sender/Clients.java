package com.dingco.dispatch.sender;

import java.net.URI;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

final class Clients {
    private Clients() {
    }

    /** AWS_ENDPOINT_URL 이 있으면(LocalStack) 그쪽으로 붙는다. 없으면 실제 AWS 기본 엔드포인트. */
    static DynamoDbClient dynamoDb() {
        var builder = DynamoDbClient.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .region(Region.of(env("AWS_REGION", "ap-northeast-2")));
        String endpoint = System.getenv("AWS_ENDPOINT_URL");
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    static S3Client s3() {
        var builder = S3Client.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .region(Region.of(env("AWS_REGION", "ap-northeast-2")));
        String endpoint = System.getenv("AWS_ENDPOINT_URL");
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        }
        return builder.build();
    }

    static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }
}

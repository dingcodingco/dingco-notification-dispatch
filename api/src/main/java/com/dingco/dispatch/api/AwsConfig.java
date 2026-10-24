package com.dingco.dispatch.api;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;

/** AWS_ENDPOINT_URL 이 있으면 LocalStack 으로 붙는다. 없으면 실제 AWS 기본 엔드포인트. */
@Configuration
public class AwsConfig {
    private final LabProperties lab;

    public AwsConfig(LabProperties lab) {
        this.lab = lab;
    }

    private boolean local() {
        return lab.endpoint() != null && !lab.endpoint().isBlank();
    }

    @Bean
    DynamoDbClient dynamoDb() {
        var b = DynamoDbClient.builder().httpClient(UrlConnectionHttpClient.create()).region(Region.of(lab.region()));
        if (local()) b.endpointOverride(URI.create(lab.endpoint()));
        return b.build();
    }

    @Bean
    S3Client s3() {
        var b = S3Client.builder().httpClient(UrlConnectionHttpClient.create()).region(Region.of(lab.region()));
        if (local()) b.endpointOverride(URI.create(lab.endpoint())).forcePathStyle(true);
        return b.build();
    }

    @Bean
    S3Presigner presigner() {
        var b = S3Presigner.builder().region(Region.of(lab.region()))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(local()).build());
        if (local()) b.endpointOverride(URI.create(lab.endpoint()));
        return b.build();
    }

    @Bean
    SqsClient sqs() {
        var b = SqsClient.builder().httpClient(UrlConnectionHttpClient.create()).region(Region.of(lab.region()));
        if (local()) b.endpointOverride(URI.create(lab.endpoint()));
        return b.build();
    }
}

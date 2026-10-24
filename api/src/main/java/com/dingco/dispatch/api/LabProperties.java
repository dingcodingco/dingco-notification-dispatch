package com.dingco.dispatch.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lab")
public record LabProperties(
        String region, String endpoint, String vendorUrl,
        String campaignsTable, String ledgerTable, String recipientsBucket, String payloadsBucket,
        String plannedQueue, String sendDlq, int uploadUrlTtlMinutes) {
}

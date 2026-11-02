package com.dingco.dispatch.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 공개 REST 계약 3개. 자세한 스키마는 openapi/notification-api.yaml. */
@RestController
public class CampaignController {
    private final CampaignService service;

    public CampaignController(CampaignService service) {
        this.service = service;
    }

    public record CreateCampaignRequest(
            @NotBlank String tenantId,
            @NotBlank String name,
            @NotBlank @Pattern(regexp = "SMS|EMAIL|PUSH") String channel,
            @NotBlank String template) { }

    @PostMapping("/campaigns")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateCampaignRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PostMapping("/campaigns/{id}/dispatch")
    public ResponseEntity<Map<String, Object>> dispatch(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.dispatch(id));
    }

    @GetMapping("/campaigns/{id}")
    public Map<String, Object> get(@PathVariable String id) {
        return service.status(id);
    }

    @ExceptionHandler(CampaignService.NotFound.class)
    ResponseEntity<Map<String, String>> notFound(CampaignService.NotFound e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(CampaignService.Conflict.class)
    ResponseEntity<Map<String, String>> conflict(CampaignService.Conflict e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }
}

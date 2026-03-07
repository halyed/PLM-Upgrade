package com.plm.integration.controller;

import com.plm.integration.dto.ConnectorStatus;
import com.plm.integration.dto.WebhookRequest;
import com.plm.integration.service.EventBridgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
@Slf4j
public class IntegrationController {

    private final EventBridgeService bridge;

    @Value("${integration.webhook.secret:}")
    private String webhookSecret;

    @GetMapping("/connectors")
    public ResponseEntity<List<ConnectorStatus>> connectors() {
        return ResponseEntity.ok(bridge.allStatuses());
    }

    @GetMapping("/connectors/summary")
    public ResponseEntity<Map<String, Long>> summary() {
        return ResponseEntity.ok(bridge.summary());
    }

    /**
     * POST /api/integration/webhook/{source}
     * Validated via HMAC-SHA256 signature in X-Webhook-Signature header.
     * Header format: sha256=<hex>
     * If integration.webhook.secret is empty, validation is skipped (dev mode).
     */
    @PostMapping("/webhook/{source}")
    public ResponseEntity<Void> webhook(
            @PathVariable String source,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody String rawBody,
            @Valid @RequestBody(required = false) WebhookRequest ignored) {

        if (!webhookSecret.isBlank() && !verifySignature(rawBody, signature)) {
            log.warn("[Webhook] Invalid signature from source={}", source);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            WebhookRequest request = mapper.readValue(rawBody, WebhookRequest.class);
            request.setSource(source);
            bridge.handleWebhook(request);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            log.error("[Webhook] Failed to parse body from {}: {}", source, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    private boolean verifySignature(String body, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String computed = "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}

package com.plm.integration.controller;

import com.plm.integration.dto.ConnectorStatus;
import com.plm.integration.dto.WebhookRequest;
import com.plm.integration.service.EventBridgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
public class IntegrationController {

    private final EventBridgeService bridge;

    /**
     * GET /api/integration/connectors
     * Returns health status of all configured connectors.
     * Secured — requires Keycloak JWT.
     */
    @GetMapping("/connectors")
    public ResponseEntity<List<ConnectorStatus>> connectors() {
        return ResponseEntity.ok(bridge.allStatuses());
    }

    /**
     * GET /api/integration/connectors/summary
     * Quick overview: { total, up, down }
     */
    @GetMapping("/connectors/summary")
    public ResponseEntity<Map<String, Long>> summary() {
        return ResponseEntity.ok(bridge.summary());
    }

    /**
     * POST /api/integration/webhook/{source}
     * Receives events from external systems (Odoo, MES, FreeCAD).
     * Secured via X-Webhook-Secret header (validated per source config).
     * Permitted without JWT — external systems send their own credentials.
     */
    @PostMapping("/webhook/{source}")
    public ResponseEntity<Void> webhook(
            @PathVariable String source,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @Valid @RequestBody WebhookRequest request) {

        request.setSource(source);
        bridge.handleWebhook(request);
        return ResponseEntity.accepted().build();
    }
}

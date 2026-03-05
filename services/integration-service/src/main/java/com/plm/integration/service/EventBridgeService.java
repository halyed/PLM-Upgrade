package com.plm.integration.service;

import com.plm.integration.config.KafkaConfig;
import com.plm.integration.connector.ExternalSystemConnector;
import com.plm.integration.dto.ConnectorStatus;
import com.plm.integration.dto.WebhookRequest;
import com.plm.integration.event.ExternalEvent;
import com.plm.integration.event.PlmEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core of the integration service.
 *
 * Inbound:  consumes PLM Kafka events → fans out to all enabled connectors
 * Outbound: receives webhook calls from external systems → publishes to plm.external-events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventBridgeService {

    private final List<ExternalSystemConnector> connectors;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── Inbound: PLM → External ──────────────────────────────────────────────

    @KafkaListener(topics = KafkaConfig.ITEM_EVENTS_TOPIC, groupId = "integration-service")
    public void onItemEvent(PlmEvent event) {
        log.info("[Bridge] Item event received: {} (id={})", event.getType(), event.getId());
        fanOut(event);
    }

    @KafkaListener(topics = KafkaConfig.WORKFLOW_EVENTS_TOPIC, groupId = "integration-service")
    public void onWorkflowEvent(PlmEvent event) {
        log.info("[Bridge] Workflow event received: {} (revisionId={})", event.getType(), event.getRevisionId());
        fanOut(event);
    }

    /** Deliver event to every enabled connector */
    private void fanOut(PlmEvent event) {
        for (ExternalSystemConnector connector : connectors) {
            try {
                connector.onPlmEvent(event);
            } catch (Exception e) {
                log.error("[Bridge] Connector {} threw an error for event {}: {}",
                        connector.getId(), event.getType(), e.getMessage());
            }
        }
    }

    // ── Outbound: External → PLM ──────────────────────────────────────────────

    /**
     * Accepts a webhook call from an external system and publishes it
     * as an ExternalEvent to plm.external-events so plm-core-service
     * (or any other service) can react.
     */
    public void handleWebhook(WebhookRequest request) {
        ExternalEvent event = ExternalEvent.builder()
                .source(request.getSource())
                .type(request.getType())
                .externalId(request.getExternalId())
                .plmItemNumber(request.getPlmItemNumber())
                .payload(request.getPayload())
                .receivedAt(Instant.now())
                .build();

        kafkaTemplate.send(KafkaConfig.EXTERNAL_EVENTS_TOPIC, request.getSource(), event);
        log.info("[Bridge] Webhook from {} published to Kafka: {}", request.getSource(), request.getType());
    }

    // ── Status ────────────────────────────────────────────────────────────────

    public List<ConnectorStatus> allStatuses() {
        return connectors.stream()
                .map(ExternalSystemConnector::status)
                .collect(Collectors.toList());
    }

    public Map<String, Long> summary() {
        long up   = connectors.stream().map(ExternalSystemConnector::status).filter(ConnectorStatus::isUp).count();
        long down = connectors.size() - up;
        return Map.of("total", (long) connectors.size(), "up", up, "down", down);
    }
}

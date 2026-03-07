package com.plm.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumes inbound events from external systems (Odoo, MES, FreeCAD)
 * published by integration-service to plm.external-events.
 */
@Service
@Slf4j
public class ExternalEventConsumer {

    @KafkaListener(topics = "plm.external-events", groupId = "plm-core-service")
    public void onExternalEvent(JsonNode event) {
        String source = event.path("source").asText();
        String type   = event.path("type").asText();
        String itemNumber = event.path("plmItemNumber").asText();
        String externalId = event.path("externalId").asText();

        log.info("[External] source={} type={} itemNumber={} externalId={}", source, type, itemNumber, externalId);

        // Extend here to react to specific external events, e.g.:
        // - PRICE_UPDATED from Odoo → update item cost metadata
        // - PRODUCTION_STARTED from MES → update item status
        // - EXPORT_COMPLETE from FreeCAD → attach generated document
    }
}

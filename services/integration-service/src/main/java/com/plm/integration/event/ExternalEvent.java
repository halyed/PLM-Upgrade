package com.plm.integration.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Event received from an external system (ERP/MES) via webhook,
 * then published to plm.external-events Kafka topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalEvent {

    private String source;        // "odoo", "mes", etc.
    private String type;          // "ORDER_CREATED", "PRODUCTION_COMPLETE", etc.
    private String externalId;    // ID in the external system
    private String plmItemNumber; // linked PLM item (if known)
    private Map<String, Object> payload;
    private Instant receivedAt;
}

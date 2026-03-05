package com.plm.integration.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Generic PLM platform event consumed from Kafka.
 * Maps to events published by plm-core-service and workflow-service.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlmEvent {

    private String type;       // ITEM_CREATED, ITEM_UPDATED, REVISION_RELEASED, REVISION_REJECTED, etc.
    private String id;
    private String itemId;
    private String revisionId;
    private String itemNumber;
    private String name;
    private String description;
    private String lifecycleState;
    private String revisionCode;
    private String status;
    private String reason;
    private String timestamp;
}

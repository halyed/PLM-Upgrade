package com.plm.integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class WebhookRequest {

    @NotBlank
    private String source;       // "odoo", "mes", "freecad"

    @NotBlank
    private String type;         // external event type

    private String externalId;
    private String plmItemNumber;
    private Map<String, Object> payload;
}

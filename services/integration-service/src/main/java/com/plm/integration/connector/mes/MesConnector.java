package com.plm.integration.connector.mes;

import com.plm.integration.connector.ExternalSystemConnector;
import com.plm.integration.dto.ConnectorStatus;
import com.plm.integration.event.PlmEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Generic MES (Manufacturing Execution System) connector.
 *
 * Notifies the MES when a revision is released so manufacturing
 * can pick up the latest approved design.
 *
 * Enable with: integration.connectors.mes.enabled=true
 * The MES must expose a REST API — configure the endpoint in properties.
 */
@Component
@ConditionalOnProperty(prefix = "integration.connectors.mes", name = "enabled", havingValue = "true")
@Slf4j
public class MesConnector implements ExternalSystemConnector {

    private final RestTemplate restTemplate;

    @Value("${integration.connectors.mes.url}")
    private String mesUrl;

    @Value("${integration.connectors.mes.api-key}")
    private String apiKey;

    public MesConnector(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override public String getId()      { return "mes"; }
    @Override public String getName()    { return "MES"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public ConnectorStatus status() {
        try {
            HttpHeaders headers = authHeaders();
            restTemplate.exchange(mesUrl + "/health", HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return ConnectorStatus.up(getId(), getName());
        } catch (Exception e) {
            return ConnectorStatus.down(getId(), getName(), e.getMessage());
        }
    }

    @Override
    public void onPlmEvent(PlmEvent event) {
        // MES only cares when a revision is officially released
        if (!"REVISION_RELEASED".equals(event.getType())) return;

        log.info("[MES] Notifying release of item {} rev {}", event.getItemNumber(), event.getRevisionCode());
        try {
            var payload = Map.of(
                "event",        "PART_RELEASED",
                "itemNumber",   event.getItemNumber(),
                "revision",     event.getRevisionCode(),
                "itemName",     event.getName() != null ? event.getName() : "",
                "plmRevisionId", event.getRevisionId()
            );
            HttpHeaders headers = authHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(mesUrl + "/api/plm-events", new HttpEntity<>(payload, headers), String.class);
            log.info("[MES] Notification sent for {} rev {}", event.getItemNumber(), event.getRevisionCode());
        } catch (Exception e) {
            log.error("[MES] Failed to notify MES for {} rev {}: {}", event.getItemNumber(), event.getRevisionCode(), e.getMessage());
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        return headers;
    }
}

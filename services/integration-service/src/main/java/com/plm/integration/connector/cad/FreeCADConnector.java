package com.plm.integration.connector.cad;

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
 * FreeCAD connector.
 *
 * Syncs PLM item metadata to a FreeCAD server running the FreeCAD
 * web API (or a custom Flask bridge). On item creation/update,
 * pushes item number and metadata so FreeCAD documents stay in sync.
 * On revision release, triggers a FreeCAD export job (STEP/PDF).
 *
 * Enable with: integration.connectors.freecad.enabled=true
 */
@Component
@ConditionalOnProperty(prefix = "integration.connectors.freecad", name = "enabled", havingValue = "true")
@Slf4j
public class FreeCADConnector implements ExternalSystemConnector {

    private final RestTemplate restTemplate;

    @Value("${integration.connectors.freecad.url}")
    private String freecadUrl;

    @Value("${integration.connectors.freecad.api-key}")
    private String apiKey;

    public FreeCADConnector(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override public String getId()      { return "freecad"; }
    @Override public String getName()    { return "FreeCAD"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public ConnectorStatus status() {
        try {
            restTemplate.exchange(
                freecadUrl + "/api/health",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                String.class
            );
            return ConnectorStatus.up(getId(), getName());
        } catch (Exception e) {
            return ConnectorStatus.down(getId(), getName(), e.getMessage());
        }
    }

    @Override
    public void onPlmEvent(PlmEvent event) {
        switch (event.getType()) {
            case "ITEM_CREATED" -> registerItem(event);
            case "ITEM_UPDATED" -> updateItem(event);
            case "REVISION_RELEASED" -> triggerExport(event);
            default -> log.debug("[FreeCAD] Ignored event: {}", event.getType());
        }
    }

    /** Register a new PLM item in FreeCAD's document registry */
    private void registerItem(PlmEvent event) {
        log.info("[FreeCAD] Registering item {} in FreeCAD", event.getItemNumber());
        try {
            var body = Map.of(
                "itemNumber",  event.getItemNumber(),
                "name",        event.getName() != null ? event.getName() : "",
                "description", event.getDescription() != null ? event.getDescription() : "",
                "action",      "register"
            );
            post("/api/items", body);
            log.info("[FreeCAD] Item {} registered", event.getItemNumber());
        } catch (Exception e) {
            log.error("[FreeCAD] Failed to register item {}: {}", event.getItemNumber(), e.getMessage());
        }
    }

    /** Sync updated metadata to FreeCAD */
    private void updateItem(PlmEvent event) {
        log.info("[FreeCAD] Updating item {} metadata in FreeCAD", event.getItemNumber());
        try {
            var body = Map.of(
                "itemNumber",  event.getItemNumber(),
                "name",        event.getName() != null ? event.getName() : "",
                "description", event.getDescription() != null ? event.getDescription() : "",
                "action",      "update"
            );
            post("/api/items", body);
        } catch (Exception e) {
            log.error("[FreeCAD] Failed to update item {}: {}", event.getItemNumber(), e.getMessage());
        }
    }

    /**
     * Trigger a FreeCAD export when a revision is released.
     * FreeCAD generates STEP + PDF drawings for manufacturing.
     */
    private void triggerExport(PlmEvent event) {
        log.info("[FreeCAD] Triggering export for item {} rev {}", event.getItemNumber(), event.getRevisionCode());
        try {
            var body = Map.of(
                "itemNumber",   event.getItemNumber(),
                "revisionCode", event.getRevisionCode(),
                "formats",      new String[]{"STEP", "PDF"},
                "action",       "export"
            );
            post("/api/export", body);
            log.info("[FreeCAD] Export triggered for {} rev {}", event.getItemNumber(), event.getRevisionCode());
        } catch (Exception e) {
            log.error("[FreeCAD] Export failed for {} rev {}: {}", event.getItemNumber(), event.getRevisionCode(), e.getMessage());
        }
    }

    private void post(String path, Object body) {
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity(freecadUrl + path, new HttpEntity<>(body, headers), String.class);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        return headers;
    }
}

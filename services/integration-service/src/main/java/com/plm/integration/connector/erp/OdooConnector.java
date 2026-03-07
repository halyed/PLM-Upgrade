package com.plm.integration.connector.erp;

import com.plm.integration.connector.ExternalSystemConnector;
import com.plm.integration.dto.ConnectorStatus;
import com.plm.integration.event.PlmEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * Odoo ERP connector.
 *
 * Sends PLM item and revision events to Odoo via its JSON-RPC API.
 * Enable with: integration.connectors.odoo.enabled=true
 *
 * Relevant Odoo modules:
 *   - mrp (Manufacturing) — maps to PLM Items/BOMs
 *   - product — maps to PLM Items
 */
@Component
@ConditionalOnProperty(prefix = "integration.connectors.odoo", name = "enabled", havingValue = "true")
@Slf4j
public class OdooConnector implements ExternalSystemConnector {

    private final RestTemplate restTemplate;

    @Value("${integration.connectors.odoo.url}")
    private String odooUrl;

    @Value("${integration.connectors.odoo.database}")
    private String database;

    @Value("${integration.connectors.odoo.username}")
    private String username;

    @Value("${integration.connectors.odoo.password}")
    private String password;

    public OdooConnector(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override public String getId()   { return "odoo"; }
    @Override public String getName() { return "Odoo ERP"; }
    @Override public boolean isEnabled() { return true; }

    @Override
    public ConnectorStatus status() {
        try {
            String versionUrl = odooUrl + "/web/webclient/version_info";
            restTemplate.getForEntity(versionUrl, String.class);
            return ConnectorStatus.up(getId(), getName());
        } catch (Exception e) {
            return ConnectorStatus.down(getId(), getName(), e.getMessage());
        }
    }

    @Override
    public void onPlmEvent(PlmEvent event) {
        switch (event.getType()) {
            case "ITEM_CREATED"     -> createOdooProduct(event);
            case "ITEM_UPDATED"     -> updateOdooProduct(event);
            case "REVISION_RELEASED" -> releaseOdooProduct(event);
            default -> log.debug("[Odoo] Ignored event type: {}", event.getType());
        }
    }

    private void createOdooProduct(PlmEvent event) {
        log.info("[Odoo] Creating product for item {} ({})", event.getItemNumber(), event.getName());
        try {
            // Odoo JSON-RPC: call product.template create
            var body = Map.of(
                "jsonrpc", "2.0",
                "method", "call",
                "params", Map.of(
                    "model", "product.template",
                    "method", "create",
                    "args", new Object[]{Map.of(
                        "name", event.getName(),
                        "default_code", event.getItemNumber(),
                        "description", event.getDescription() != null ? event.getDescription() : "",
                        "type", "product"
                    )},
                    "kwargs", Map.of()
                )
            );
            callOdooRpc(body);
            log.info("[Odoo] Product created for item {}", event.getItemNumber());
        } catch (Exception e) {
            log.error("[Odoo] Failed to create product for item {}: {}", event.getItemNumber(), e.getMessage());
        }
    }

    private void updateOdooProduct(PlmEvent event) {
        log.info("[Odoo] Updating product for item {}", event.getItemNumber());
        try {
            JsonNode searchResult = callOdooRpc(Map.of(
                "jsonrpc", "2.0", "method", "call",
                "params", Map.of(
                    "model", "product.template", "method", "search_read",
                    "args", new Object[]{new Object[]{new Object[]{"default_code", "=", event.getItemNumber()}}},
                    "kwargs", Map.of("fields", new String[]{"id"}, "limit", 1)
                )
            ));
            var records = searchResult.path("result");
            if (records.isArray() && records.size() > 0) {
                int productId = records.get(0).path("id").asInt();
                callOdooRpc(Map.of(
                    "jsonrpc", "2.0", "method", "call",
                    "params", Map.of(
                        "model", "product.template", "method", "write",
                        "args", new Object[]{new int[]{productId}, Map.of(
                            "name", event.getName() != null ? event.getName() : "",
                            "description", event.getDescription() != null ? event.getDescription() : ""
                        )},
                        "kwargs", Map.of()
                    )
                ));
                log.info("[Odoo] Product updated for item {}", event.getItemNumber());
            } else {
                log.warn("[Odoo] Product not found for item {} — creating", event.getItemNumber());
                createOdooProduct(event);
            }
        } catch (Exception e) {
            log.error("[Odoo] Failed to update product for item {}: {}", event.getItemNumber(), e.getMessage());
        }
    }

    private void releaseOdooProduct(PlmEvent event) {
        log.info("[Odoo] Releasing product {} rev {}", event.getItemNumber(), event.getRevisionCode());
        try {
            JsonNode searchResult = callOdooRpc(Map.of(
                "jsonrpc", "2.0", "method", "call",
                "params", Map.of(
                    "model", "product.template", "method", "search_read",
                    "args", new Object[]{new Object[]{new Object[]{"default_code", "=", event.getItemNumber()}}},
                    "kwargs", Map.of("fields", new String[]{"id"}, "limit", 1)
                )
            ));
            var records = searchResult.path("result");
            if (records.isArray() && records.size() > 0) {
                int productId = records.get(0).path("id").asInt();
                callOdooRpc(Map.of(
                    "jsonrpc", "2.0", "method", "call",
                    "params", Map.of(
                        "model", "product.template", "method", "write",
                        "args", new Object[]{new int[]{productId}, Map.of(
                            "description_sale", "Released — Rev " + (event.getRevisionCode() != null ? event.getRevisionCode() : ""),
                            "active", true
                        )},
                        "kwargs", Map.of()
                    )
                ));
                log.info("[Odoo] Product released for item {} rev {}", event.getItemNumber(), event.getRevisionCode());
            }
        } catch (Exception e) {
            log.error("[Odoo] Failed to release product for item {}: {}", event.getItemNumber(), e.getMessage());
        }
    }

    private JsonNode callOdooRpc(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var response = restTemplate.postForEntity(odooUrl + "/web/dataset/call_kw",
                new HttpEntity<>(body, headers), JsonNode.class);
        return response.getBody() != null ? response.getBody() : com.fasterxml.jackson.databind.node.NullNode.getInstance();
    }
}

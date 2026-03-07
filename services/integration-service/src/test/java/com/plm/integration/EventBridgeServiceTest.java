package com.plm.integration;

import com.plm.integration.connector.ExternalSystemConnector;
import com.plm.integration.dto.ConnectorStatus;
import com.plm.integration.dto.WebhookRequest;
import com.plm.integration.event.PlmEvent;
import com.plm.integration.service.EventBridgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class EventBridgeServiceTest {

    @Autowired EventBridgeService eventBridge;
    @MockBean KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void allStatuses_returnsEmptyWhenNoConnectors() {
        List<ConnectorStatus> statuses = eventBridge.allStatuses();
        // No connectors enabled in test profile
        assertThat(statuses).isEmpty();
    }

    @Test
    void summary_returnsZeroWhenNoConnectors() {
        Map<String, Long> summary = eventBridge.summary();
        assertThat(summary.get("total")).isEqualTo(0L);
        assertThat(summary.get("up")).isEqualTo(0L);
    }

    @Test
    void handleWebhook_publishesToKafka() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        WebhookRequest req = new WebhookRequest();
        req.setSource("odoo");
        req.setType("PRICE_UPDATED");
        req.setExternalId("EXT-001");
        req.setPlmItemNumber("PLM-001");

        eventBridge.handleWebhook(req);

        verify(kafkaTemplate, times(1)).send(eq("plm.external-events"), eq("odoo"), any());
    }
}

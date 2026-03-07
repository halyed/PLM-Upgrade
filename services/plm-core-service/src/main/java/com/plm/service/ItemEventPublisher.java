package com.plm.service;

import com.plm.config.KafkaConfig;
import com.plm.entity.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishItemCreated(Item item) {
        send("ITEM_CREATED", item);
    }

    public void publishItemUpdated(Item item) {
        send("ITEM_UPDATED", item);
    }

    public void publishItemDeleted(Long id, String itemNumber) {
        var event = Map.of(
                "type", "ITEM_DELETED",
                "id", String.valueOf(id),
                "itemNumber", itemNumber,
                "timestamp", Instant.now().toString()
        );
        kafkaTemplate.send(KafkaConfig.ITEM_EVENTS_TOPIC, String.valueOf(id), event);
        log.info("Published ITEM_DELETED for item {}", itemNumber);
    }

    public void publishLifecycleChanged(Item item) {
        send("LIFECYCLE_CHANGED", item);
    }

    private void send(String type, Item item) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("id", String.valueOf(item.getId()));
        event.put("itemNumber", item.getItemNumber());
        event.put("name", item.getName() != null ? item.getName() : "");
        event.put("description", item.getDescription() != null ? item.getDescription() : "");
        event.put("lifecycleState", item.getLifecycleState() != null ? item.getLifecycleState().name() : "");
        event.put("timestamp", Instant.now().toString());
        kafkaTemplate.send(KafkaConfig.ITEM_EVENTS_TOPIC, String.valueOf(item.getId()), event);
        log.info("Published {} for item {}", type, item.getItemNumber());
    }
}

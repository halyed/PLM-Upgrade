package com.plm.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.plm.search.document.ItemDocument;
import com.plm.search.document.RevisionDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Listens to Kafka events from plm-core-service and workflow-service
 * to keep the Elasticsearch index in sync.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingConsumer {

    private final ItemSearchRepository itemRepository;

    @KafkaListener(topics = "plm.item-events", groupId = "search-service")
    public void onItemEvent(JsonNode event) {
        String type = event.path("type").asText();
        log.info("Received item event: {}", type);

        switch (type) {
            case "ITEM_CREATED", "ITEM_UPDATED" -> {
                ItemDocument doc = ItemDocument.builder()
                        .id(event.path("id").asText())
                        .itemNumber(event.path("itemNumber").asText())
                        .name(event.path("name").asText())
                        .description(event.path("description").asText())
                        .lifecycleState(event.path("lifecycleState").asText())
                        .createdAt(event.path("createdAt").asText())
                        .build();
                itemRepository.save(doc);
                log.info("Indexed item {}", doc.getId());
            }
            case "ITEM_DELETED" -> {
                String id = event.path("id").asText();
                itemRepository.deleteById(id);
                log.info("Removed item {} from index", id);
            }
            default -> log.debug("Unhandled item event type: {}", type);
        }
    }

    @KafkaListener(topics = "plm.workflow-events", groupId = "search-service")
    public void onWorkflowEvent(JsonNode event) {
        // When a revision is released via workflow, update lifecycle state in index
        String type = event.path("type").asText();
        if ("REVISION_RELEASED".equals(type)) {
            String itemId = event.path("itemId").asText();
            itemRepository.findById(itemId).ifPresent(doc -> {
                doc.setLifecycleState("RELEASED");
                itemRepository.save(doc);
                log.info("Updated item {} lifecycle to RELEASED in index", itemId);
            });
        }
    }
}

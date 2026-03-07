package com.plm.notification.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.plm.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlmEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "plm.item-events", groupId = "notification-service")
    public void onItemEvent(JsonNode event) {
        String type       = event.path("type").asText();
        String itemNumber = event.path("itemNumber").asText();
        String name       = event.path("name").asText();
        String state      = event.path("lifecycleState").asText();
        String entityId   = event.path("id").asText();

        log.info("[Notifier] Item event: {} itemNumber={}", type, itemNumber);

        switch (type) {
            case "ITEM_CREATED" -> notificationService.create(
                    type, "New item created",
                    "Item " + itemNumber + " (" + name + ") has been created.",
                    "item", entityId, "ALL");

            case "LIFECYCLE_CHANGED" -> notificationService.create(
                    type, "Lifecycle state changed",
                    "Item " + itemNumber + " moved to " + state + ".",
                    "item", entityId, "ALL");

            case "ITEM_DELETED" -> notificationService.create(
                    type, "Item deleted",
                    "Item " + itemNumber + " has been deleted.",
                    "item", entityId, "ALL");

            default -> log.debug("[Notifier] Unhandled item event type: {}", type);
        }
    }

    @KafkaListener(topics = "plm.workflow-events", groupId = "notification-service")
    public void onWorkflowEvent(JsonNode event) {
        String type       = event.path("type").asText();
        String revisionId = event.path("revisionId").asText();
        String itemId     = event.path("itemId").asText();

        log.info("[Notifier] Workflow event: {} revisionId={}", type, revisionId);

        switch (type) {
            case "REVISION_RELEASED" -> notificationService.create(
                    type, "Revision released",
                    "Revision " + revisionId + " has been approved and released.",
                    "revision", revisionId, "ALL");

            case "REVISION_REJECTED" -> {
                String reason = event.path("reason").asText("No reason provided");
                notificationService.create(
                        type, "Revision rejected",
                        "Revision " + revisionId + " was rejected: " + reason,
                        "revision", revisionId, "ALL");
            }

            default -> log.debug("[Notifier] Unhandled workflow event type: {}", type);
        }
    }
}

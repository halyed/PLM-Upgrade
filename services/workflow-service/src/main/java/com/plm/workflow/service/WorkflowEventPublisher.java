package com.plm.workflow.service;

import com.plm.workflow.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishRevisionReleased(Long revisionId, Long itemId) {
        var event = Map.of(
                "type", "REVISION_RELEASED",
                "revisionId", revisionId,
                "itemId", itemId
        );
        kafkaTemplate.send(KafkaConfig.WORKFLOW_EVENTS_TOPIC, String.valueOf(revisionId), event);
        log.info("Published REVISION_RELEASED event for revision {}", revisionId);
    }

    public void publishRevisionRejected(Long revisionId, Long itemId, String reason) {
        var event = Map.of(
                "type", "REVISION_REJECTED",
                "revisionId", revisionId,
                "itemId", itemId,
                "reason", reason != null ? reason : ""
        );
        kafkaTemplate.send(KafkaConfig.WORKFLOW_EVENTS_TOPIC, String.valueOf(revisionId), event);
        log.info("Published REVISION_REJECTED event for revision {}", revisionId);
    }
}

package com.plm.workflow.worker;

import com.plm.workflow.service.WorkflowEventPublisher;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotifyRejectionWorker {

    private final WorkflowEventPublisher eventPublisher;

    @JobWorker(type = "notify-rejection")
    public void notifyRejection(JobClient client, ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        Long revisionId = (Long) vars.get("revisionId");
        Long itemId = (Long) vars.get("itemId");
        String reason = (String) vars.getOrDefault("rejectionComment", "No reason provided");

        log.info("Notifying rejection for revision {} item {}: {}", revisionId, itemId, reason);
        eventPublisher.publishRevisionRejected(revisionId, itemId, reason);

        client.newCompleteCommand(job.getKey()).send().join();
    }
}

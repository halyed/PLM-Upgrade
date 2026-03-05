package com.plm.workflow.worker;

import com.plm.workflow.service.WorkflowEventPublisher;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReleaseRevisionWorker {

    private final WorkflowEventPublisher eventPublisher;

    @JobWorker(type = "release-revision")
    public void releaseRevision(JobClient client, ActivatedJob job) {
        Long revisionId = (Long) job.getVariablesAsMap().get("revisionId");
        Long itemId = (Long) job.getVariablesAsMap().get("itemId");

        log.info("Releasing revision {} for item {}", revisionId, itemId);
        eventPublisher.publishRevisionReleased(revisionId, itemId);

        client.newCompleteCommand(job.getKey()).send().join();
        log.info("Revision {} released successfully", revisionId);
    }
}

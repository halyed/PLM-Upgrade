package com.plm.workflow.worker;

import com.plm.workflow.dto.PendingTask;
import com.plm.workflow.service.WorkflowInstanceStore;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Picks up the manager-review-task job and stores it without completing.
 * The task is completed later via POST /api/workflows/tasks/{jobKey}/complete.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ManagerReviewWorker {

    private final WorkflowInstanceStore store;

    @JobWorker(type = "manager-review-task", timeout = 86400000L /* 24h */, autoComplete = false)
    public void onManagerReview(JobClient client, ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        Long revisionId  = toLong(vars.get("revisionId"));
        Long itemId      = toLong(vars.get("itemId"));
        String itemName  = (String) vars.getOrDefault("itemName", "");
        String revCode   = (String) vars.getOrDefault("revisionCode", "");
        String submitter = (String) vars.getOrDefault("submittedBy", "unknown");
        String pik       = String.valueOf(job.getProcessInstanceKey());

        log.info("Manager review task received — jobKey={} revision={}", job.getKey(), revisionId);

        store.savePendingTask(new PendingTask(
                job.getKey(), "MANAGER_REVIEW", pik,
                revisionId, itemId, itemName, revCode, submitter, Instant.now()
        ));
        store.updateStatus(pik, "RUNNING", "MANAGER_REVIEW");
        // Do NOT complete — wait for REST call
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}

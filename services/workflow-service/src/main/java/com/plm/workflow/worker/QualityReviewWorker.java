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
 * Picks up the quality-review-task job and stores it without completing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QualityReviewWorker {

    private final WorkflowInstanceStore store;

    @JobWorker(type = "quality-review-task", timeout = 86400000L /* 24h */, autoComplete = false)
    public void onQualityReview(JobClient client, ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        Long revisionId  = toLong(vars.get("revisionId"));
        Long itemId      = toLong(vars.get("itemId"));
        String itemName  = (String) vars.getOrDefault("itemName", "");
        String revCode   = (String) vars.getOrDefault("revisionCode", "");
        String submitter = (String) vars.getOrDefault("submittedBy", "unknown");
        String pik       = String.valueOf(job.getProcessInstanceKey());

        log.info("Quality review task received — jobKey={} revision={}", job.getKey(), revisionId);

        store.savePendingTask(new PendingTask(
                job.getKey(), "QUALITY_REVIEW", pik,
                revisionId, itemId, itemName, revCode, submitter, Instant.now()
        ));
        store.updateStatus(pik, "RUNNING", "QUALITY_REVIEW");
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}

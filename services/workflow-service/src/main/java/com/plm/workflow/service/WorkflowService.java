package com.plm.workflow.service;

import com.plm.workflow.dto.StartWorkflowRequest;
import com.plm.workflow.dto.WorkflowInstance;
import com.plm.workflow.dto.WorkflowResponse;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {

    private final ZeebeClient zeebeClient;
    private final WorkflowInstanceStore store;

    public WorkflowResponse startApprovalWorkflow(StartWorkflowRequest request) {
        log.info("Starting approval workflow for revision {} (item {})",
                request.getRevisionId(), request.getItemId());

        ProcessInstanceEvent instance = zeebeClient
                .newCreateInstanceCommand()
                .bpmnProcessId("revision-approval")
                .latestVersion()
                .variables(Map.of(
                        "revisionId", request.getRevisionId(),
                        "itemId", request.getItemId(),
                        "itemName", request.getItemName() != null ? request.getItemName() : "",
                        "revisionCode", request.getRevisionCode() != null ? request.getRevisionCode() : "",
                        "submittedBy", request.getSubmittedBy() != null ? request.getSubmittedBy() : "unknown"
                ))
                .send()
                .join();

        String key = String.valueOf(instance.getProcessInstanceKey());
        log.info("Workflow started: processInstanceKey={}", key);

        store.save(new WorkflowInstance(
                key,
                request.getRevisionId(),
                request.getItemId(),
                request.getItemName(),
                request.getRevisionCode(),
                request.getSubmittedBy(),
                "RUNNING",
                "MANAGER_REVIEW",
                Instant.now()
        ));

        return new WorkflowResponse(key, request.getRevisionId(), "STARTED",
                "Approval workflow started — awaiting manager review");
    }
}

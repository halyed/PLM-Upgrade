package com.plm.workflow.service;

import com.plm.workflow.dto.StartWorkflowRequest;
import com.plm.workflow.dto.WorkflowResponse;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {

    private final ZeebeClient zeebeClient;

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

        log.info("Workflow started: processInstanceKey={}", instance.getProcessInstanceKey());

        return new WorkflowResponse(
                String.valueOf(instance.getProcessInstanceKey()),
                request.getRevisionId(),
                "STARTED",
                "Approval workflow started — awaiting manager review"
        );
    }
}

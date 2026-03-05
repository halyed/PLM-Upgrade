package com.plm.workflow.controller;

import com.plm.workflow.dto.StartWorkflowRequest;
import com.plm.workflow.dto.WorkflowInstance;
import com.plm.workflow.dto.WorkflowResponse;
import com.plm.workflow.service.WorkflowInstanceStore;
import com.plm.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@Slf4j
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowInstanceStore store;

    @PostMapping("/revisions/{revisionId}/start")
    public ResponseEntity<WorkflowResponse> startApproval(
            @PathVariable Long revisionId,
            @Valid @RequestBody StartWorkflowRequest request) {
        request.setRevisionId(revisionId);
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.startApprovalWorkflow(request));
        } catch (Exception e) {
            log.error("Failed to start workflow for revision {}: {}", revisionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new WorkflowResponse(null, revisionId, "UNAVAILABLE",
                            "Workflow engine unavailable: " + e.getMessage()));
        }
    }

    @GetMapping("/revisions/{revisionId}")
    public ResponseEntity<List<WorkflowInstance>> getByRevision(@PathVariable Long revisionId) {
        return ResponseEntity.ok(store.findByRevision(revisionId));
    }

    @GetMapping
    public ResponseEntity<List<WorkflowInstance>> getAll() {
        return ResponseEntity.ok(store.findAll());
    }
}

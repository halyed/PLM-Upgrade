package com.plm.workflow.controller;

import com.plm.workflow.dto.StartWorkflowRequest;
import com.plm.workflow.dto.WorkflowResponse;
import com.plm.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    /**
     * Start an approval workflow for a revision.
     * Called by plm-core-service when an engineer submits a revision for review.
     *
     * POST /api/workflows/revisions/{revisionId}/start
     */
    @PostMapping("/revisions/{revisionId}/start")
    public ResponseEntity<WorkflowResponse> startApproval(
            @PathVariable Long revisionId,
            @Valid @RequestBody StartWorkflowRequest request) {
        request.setRevisionId(revisionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.startApprovalWorkflow(request));
    }
}

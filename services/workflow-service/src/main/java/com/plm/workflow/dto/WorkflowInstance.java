package com.plm.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class WorkflowInstance {
    private String processInstanceKey;
    private Long revisionId;
    private Long itemId;
    private String itemName;
    private String revisionCode;
    private String submittedBy;
    private String status;   // RUNNING | COMPLETED | REJECTED
    private String currentStep; // MANAGER_REVIEW | QUALITY_REVIEW | RELEASED | REJECTED
    private Instant startedAt;
}

package com.plm.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class PendingTask {
    private long jobKey;
    private String taskType;        // MANAGER_REVIEW | QUALITY_REVIEW
    private String processInstanceKey;
    private Long revisionId;
    private Long itemId;
    private String itemName;
    private String revisionCode;
    private String submittedBy;
    private Instant createdAt;
}

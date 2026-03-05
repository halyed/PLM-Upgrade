package com.plm.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkflowResponse {
    private String processInstanceKey;
    private Long revisionId;
    private String status;
    private String message;
}

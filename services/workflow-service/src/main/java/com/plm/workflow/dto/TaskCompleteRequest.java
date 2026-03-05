package com.plm.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskCompleteRequest {

    @NotNull
    private Long taskId;

    @NotBlank
    // "APPROVED" or "REJECTED"
    private String decision;

    private String comment;
    private String completedBy;
}

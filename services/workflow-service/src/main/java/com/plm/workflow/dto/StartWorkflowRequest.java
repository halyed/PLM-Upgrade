package com.plm.workflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartWorkflowRequest {

    @NotNull
    private Long revisionId;

    @NotNull
    private Long itemId;

    private String itemName;
    private String revisionCode;
    private String submittedBy;
}

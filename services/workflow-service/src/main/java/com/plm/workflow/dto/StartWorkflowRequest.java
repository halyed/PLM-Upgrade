package com.plm.workflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartWorkflowRequest {

    // Set from path variable by the controller — not validated in request body
    private Long revisionId;

    @NotNull
    private Long itemId;

    private String itemName;
    private String revisionCode;
    private String submittedBy;
}

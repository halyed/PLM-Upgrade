package com.plm.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CompleteTaskRequest {
    @NotBlank
    @Pattern(regexp = "APPROVED|REJECTED")
    private String decision;
    private String comment;
}

package com.plm.dto;

import com.plm.entity.RevisionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RevisionRequest {
    @NotBlank
    @Size(max = 10)
    private String revisionCode;

    private RevisionStatus status;
}

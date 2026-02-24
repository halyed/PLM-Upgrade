package com.plm.dto;

import com.plm.entity.ChangeRequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeRequestRequest {
    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    private ChangeRequestStatus status;
}

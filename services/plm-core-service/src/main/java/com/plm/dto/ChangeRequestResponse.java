package com.plm.dto;

import com.plm.entity.ChangeRequestStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChangeRequestResponse {
    private Long id;
    private String title;
    private String description;
    private ChangeRequestStatus status;
    private Long linkedItemId;
    private String submittedBy;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

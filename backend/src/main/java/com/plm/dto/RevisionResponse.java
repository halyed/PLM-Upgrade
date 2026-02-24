package com.plm.dto;

import com.plm.entity.RevisionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RevisionResponse {
    private Long id;
    private Long itemId;
    private String itemNumber;
    private String revisionCode;
    private RevisionStatus status;
    private LocalDateTime createdAt;
}

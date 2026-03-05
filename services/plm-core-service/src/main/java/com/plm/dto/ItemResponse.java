package com.plm.dto;

import com.plm.entity.LifecycleState;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ItemResponse {
    private Long id;
    private String itemNumber;
    private String name;
    private String description;
    private LifecycleState lifecycleState;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

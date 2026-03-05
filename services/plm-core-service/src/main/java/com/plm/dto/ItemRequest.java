package com.plm.dto;

import com.plm.entity.LifecycleState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ItemRequest {
    @NotBlank
    @Size(max = 50)
    private String itemNumber;

    @NotBlank
    @Size(max = 255)
    private String name;

    private String description;

    private LifecycleState lifecycleState;
}

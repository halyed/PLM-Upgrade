package com.plm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BomLinkRequest {
    @NotNull
    private Long childRevisionId;

    @DecimalMin(value = "0.0001")
    private BigDecimal quantity = BigDecimal.ONE;
}

package com.plm.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BomLinkResponse {
    private Long id;
    private Long parentRevisionId;
    private Long childRevisionId;
    private String childItemNumber;
    private String childRevisionCode;
    private BigDecimal quantity;
}

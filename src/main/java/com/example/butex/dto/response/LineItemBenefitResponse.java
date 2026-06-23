package com.example.butex.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class LineItemBenefitResponse {

    private String itemId;
    private String categoryId;
    private BigDecimal lineTotal;
    private BigDecimal appliedDiscountPercent;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
}

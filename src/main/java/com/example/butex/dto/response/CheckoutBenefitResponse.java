package com.example.butex.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CheckoutBenefitResponse {

    private boolean membershipActive;
    private String planCode;
    private String tierCode;
    private boolean freeDelivery;
    private boolean exclusiveDealsAccess;
    private boolean earlySaleAccess;
    private boolean prioritySupport;
    private BigDecimal cartSubtotal;
    private BigDecimal totalDiscountAmount;
    private BigDecimal finalPayableAmount;
    private List<LineItemBenefitResponse> items;
}

package com.example.butex.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PlanDetailsResponse {

    private Long id;
    private Long planId;
    private Integer version;
    private Integer durationDays;
    private BigDecimal price;
    private String currency;
    private boolean freeDeliveryEnabled;
    private BigDecimal extraDiscountPercent;
    private boolean exclusiveDealsAccess;
    private boolean earlySaleAccess;
    private boolean prioritySupport;
    private boolean isDefault;
}

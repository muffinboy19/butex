package com.example.butex.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierDetailsResponse {

    private Long id;
    private Long tierId;
    private Integer version;
    private Integer minOrders;
    private BigDecimal minMonthlyOrderValue;
    private String cohortCode;
    private boolean freeDeliveryEnabled;
    private BigDecimal extraDiscountPercent;
    private boolean exclusiveDealsAccess;
    private boolean earlySaleAccess;
    private boolean prioritySupport;
    private boolean isDefault;
}

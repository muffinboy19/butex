package com.example.butex.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CreateTierDetailsRequest {

    private Integer minOrders;

    private BigDecimal minMonthlyOrderValue;

    private String cohortCode;

    private Boolean freeDeliveryEnabled;

    private BigDecimal extraDiscountPercent;

    private Boolean exclusiveDealsAccess;

    private Boolean earlySaleAccess;

    private Boolean prioritySupport;

    @NotNull(message = "effectiveFrom is required")
    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private String changeNotes;
}

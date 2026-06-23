package com.example.butex.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class UpdatePlanDetailsRequest {

    @Positive(message = "durationDays must be positive")
    private Integer durationDays;

    @Positive(message = "price must be positive")
    private BigDecimal price;

    private String currency;

    private Boolean freeDeliveryEnabled;

    private BigDecimal extraDiscountPercent;

    private Boolean exclusiveDealsAccess;

    private Boolean earlySaleAccess;

    private Boolean prioritySupport;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private String changeNotes;
}

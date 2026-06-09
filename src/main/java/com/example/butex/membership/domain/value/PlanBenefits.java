package com.example.butex.membership.domain.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanBenefits {

    @Column(nullable = false)
    private boolean freeDeliveryEnabled;

    @Column(precision = 5, scale = 2)
    private BigDecimal extraDiscountPercent;

    @Column(nullable = false)
    private boolean exclusiveDealsAccess;

    @Column(nullable = false)
    private boolean earlySaleAccess;

    @Column(nullable = false)
    private boolean prioritySupport;

    @Column(length = 2000)
    private String applicableCategories;

    @Column(length = 2000)
    private String additionalPerksJson;
}

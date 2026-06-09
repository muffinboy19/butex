package com.example.butex.membership.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tiers",
        uniqueConstraints = @UniqueConstraint(name = "uk_tier_code", columnNames = "code")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(nullable = false)
    private Integer rank;

    @Column(name = "min_orders")
    private Integer minOrders;

    @Column(name = "min_monthly_order_value", precision = 12, scale = 2)
    private BigDecimal minMonthlyOrderValue;

    @Column(name = "cohort_code", length = 64)
    private String cohortCode;

    @Builder.Default
    @Column(nullable = false)
    private boolean freeDeliveryEnabled = false;

    @Column(name = "extra_discount_percent", precision = 5, scale = 2)
    private BigDecimal extraDiscountPercent;

    @Builder.Default
    @Column(nullable = false)
    private boolean exclusiveDealsAccess = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean earlySaleAccess = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean prioritySupport = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

package com.example.butex.entity;

import com.example.butex.enums.PlanDetailsStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "tier_details",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tier_details_tier_version",
                columnNames = {"tier_id", "version"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TierDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tier_id", nullable = false)
    private Long tierId;

    @Column(nullable = false)
    private Integer version;

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

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PlanDetailsStatus status = PlanDetailsStatus.DRAFT;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(length = 512)
    private String changeNotes;

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

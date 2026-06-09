package com.example.butex.membership.domain.entity;

import com.example.butex.membership.domain.enums.PlanDetailsStatus;
import com.example.butex.membership.domain.value.PlanBenefits;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "plan_details",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_plan_details_plan_version",
                columnNames = {"plan_id", "version"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class PlanDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false)
    private Integer durationDays;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Embedded
    private PlanBenefits benefits;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PlanDetailsStatus status = PlanDetailsStatus.DRAFT;

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
        log.debug("Created plan details planId={} version={} durationDays={}", planId, version, durationDays);
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
        log.debug("Updated plan details id={} planId={} version={}", id, planId, version);
    }
}

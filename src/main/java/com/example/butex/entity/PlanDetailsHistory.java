package com.example.butex.entity;

import com.example.butex.enums.PlanDetailsHistoryAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "plan_details_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanDetailsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_details_id", nullable = false)
    private Long planDetailsId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PlanDetailsHistoryAction action;

    @Column(length = 512)
    private String remark;

    @Column(name = "action_by", nullable = false, length = 128)
    private String actionBy;

    @Column(name = "action_at", nullable = false, updatable = false)
    private LocalDateTime actionAt;

    @PrePersist
    void onCreate() {
        if (actionAt == null) {
            actionAt = LocalDateTime.now();
        }
    }
}

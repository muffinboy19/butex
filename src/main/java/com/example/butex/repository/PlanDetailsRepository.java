package com.example.butex.repository;

import com.example.butex.entity.PlanDetails;
import com.example.butex.enums.PlanDetailsStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanDetailsRepository extends JpaRepository<PlanDetails, Long> {

    List<PlanDetails> findByPlanIdAndStatus(Long planId, PlanDetailsStatus status);

    Optional<PlanDetails> findByPlanIdAndStatusAndIsDefaultTrue(Long planId, PlanDetailsStatus status);
}

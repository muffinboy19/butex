package com.example.butex.repository;

import com.example.butex.entity.PlanDetails;
import com.example.butex.enums.PlanDetailsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlanDetailsRepository extends JpaRepository<PlanDetails, Long> {

    List<PlanDetails> findByPlanId(Long planId);

    List<PlanDetails> findByPlanIdAndStatus(Long planId, PlanDetailsStatus status);

    Optional<PlanDetails> findByPlanIdAndStatusAndIsDefaultTrue(Long planId, PlanDetailsStatus status);

    @Query("SELECT COALESCE(MAX(pd.version), 0) FROM PlanDetails pd WHERE pd.planId = :planId")
    int findMaxVersionByPlanId(@Param("planId") Long planId);
}

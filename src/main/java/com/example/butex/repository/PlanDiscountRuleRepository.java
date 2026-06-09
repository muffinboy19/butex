package com.example.butex.repository;

import com.example.butex.entity.PlanDiscountRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanDiscountRuleRepository extends JpaRepository<PlanDiscountRule, Long> {

    List<PlanDiscountRule> findByPlanDetailsIdAndActiveTrue(Long planDetailsId);
}

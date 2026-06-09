package com.example.butex.repository;

import com.example.butex.entity.TierDiscountRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TierDiscountRuleRepository extends JpaRepository<TierDiscountRule, Long> {

    List<TierDiscountRule> findByTierDetailsIdAndActiveTrue(Long tierDetailsId);
}

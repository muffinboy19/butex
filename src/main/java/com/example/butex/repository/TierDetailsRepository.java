package com.example.butex.repository;

import com.example.butex.entity.TierDetails;
import com.example.butex.enums.PlanDetailsStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TierDetailsRepository extends JpaRepository<TierDetails, Long> {

    List<TierDetails> findByTierIdAndStatus(Long tierId, PlanDetailsStatus status);

    Optional<TierDetails> findByTierIdAndStatusAndIsDefaultTrue(Long tierId, PlanDetailsStatus status);
}

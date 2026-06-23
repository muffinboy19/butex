package com.example.butex.repository;

import com.example.butex.entity.TierDetails;
import com.example.butex.enums.PlanDetailsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TierDetailsRepository extends JpaRepository<TierDetails, Long> {

    List<TierDetails> findByTierId(Long tierId);

    List<TierDetails> findByTierIdAndStatus(Long tierId, PlanDetailsStatus status);

    Optional<TierDetails> findByTierIdAndStatusAndIsDefaultTrue(Long tierId, PlanDetailsStatus status);

    @Query("SELECT COALESCE(MAX(td.version), 0) FROM TierDetails td WHERE td.tierId = :tierId")
    int findMaxVersionByTierId(@Param("tierId") Long tierId);
}

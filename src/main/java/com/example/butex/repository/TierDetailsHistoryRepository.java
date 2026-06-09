package com.example.butex.repository;

import com.example.butex.entity.TierDetailsHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TierDetailsHistoryRepository extends JpaRepository<TierDetailsHistory, Long> {

    List<TierDetailsHistory> findByTierDetailsIdOrderByActionAtDesc(Long tierDetailsId);
}

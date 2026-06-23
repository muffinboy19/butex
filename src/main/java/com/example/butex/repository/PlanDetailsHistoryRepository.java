package com.example.butex.repository;

import com.example.butex.entity.PlanDetailsHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanDetailsHistoryRepository extends JpaRepository<PlanDetailsHistory, Long> {

    List<PlanDetailsHistory> findByPlanDetailsIdOrderByActionAtDesc(Long planDetailsId);
}

package com.example.butex.repository;

import com.example.butex.entity.Tier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TierRepository extends JpaRepository<Tier, Long> {

    List<Tier> findByActiveTrueOrderByRankAsc();

    Optional<Tier> findByCode(String code);
}

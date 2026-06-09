package com.example.butex.repository;

import com.example.butex.entity.UserSubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserSubscriptionHistoryRepository extends JpaRepository<UserSubscriptionHistory, Long> {

    List<UserSubscriptionHistory> findByUserIdOrderByActionAtDesc(Long userId);
}

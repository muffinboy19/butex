package com.example.butex.repository;

import com.example.butex.entity.UserOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface UserOrderRepository extends JpaRepository<UserOrder, Long> {

    long countByUserId(Long userId);

    @Query("""
            SELECT COALESCE(SUM(o.orderValue), 0)
            FROM UserOrder o
            WHERE o.userId = :userId
              AND o.orderedAt >= :from
              AND o.orderedAt < :to
            """)
    BigDecimal sumOrderValueForUserBetween(@Param("userId") Long userId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);
}

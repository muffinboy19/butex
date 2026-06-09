package com.example.butex.service;

import com.example.butex.entity.Tier;
import com.example.butex.entity.TierDetails;
import com.example.butex.entity.User;
import com.example.butex.exception.BusinessException;
import com.example.butex.repository.TierRepository;
import com.example.butex.repository.UserOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TierEligibilityService {

    private final UserOrderRepository userOrderRepository;
    private final TierRepository tierRepository;

    @Transactional(readOnly = true)
    public void requireEligible(User user, TierDetails tierDetails) {
        if (!qualifies(user, tierDetails)) {
            Tier tier = tierRepository.findById(tierDetails.getTierId())
                    .orElseThrow(() -> new BusinessException("Tier not found"));
            throw new BusinessException("User does not qualify for tier: " + tier.getCode());
        }
    }

    @Transactional(readOnly = true)
    public boolean qualifies(User user, TierDetails tierDetails) {
        long totalOrders = userOrderRepository.countByUserId(user.getId());
        BigDecimal monthlySpend = monthlyOrderValue(user.getId());
        return qualifies(user, tierDetails, totalOrders, monthlySpend);
    }

    public boolean qualifies(User user, TierDetails tierDetails, long totalOrders, BigDecimal monthlySpend) {
        if (tierDetails.getMinOrders() != null && totalOrders < tierDetails.getMinOrders()) {
            return false;
        }
        if (tierDetails.getMinMonthlyOrderValue() != null
                && monthlySpend.compareTo(tierDetails.getMinMonthlyOrderValue()) < 0) {
            return false;
        }
        if (tierDetails.getCohortCode() != null && !tierDetails.getCohortCode().isBlank()) {
            if (user.getCohortCode() == null
                    || !tierDetails.getCohortCode().equalsIgnoreCase(user.getCohortCode())) {
                return false;
            }
        }
        return true;
    }

    public BigDecimal monthlyOrderValue(Long userId) {
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDateTime monthStart = firstDay.atStartOfDay();
        LocalDateTime monthEnd = firstDay.plusMonths(1).atStartOfDay();
        return userOrderRepository.sumOrderValueForUserBetween(userId, monthStart, monthEnd);
    }
}

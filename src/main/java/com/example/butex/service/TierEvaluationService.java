package com.example.butex.service;

import com.example.butex.entity.Subscription;
import com.example.butex.entity.Tier;
import com.example.butex.entity.TierDetails;
import com.example.butex.entity.User;
import com.example.butex.enums.PlanDetailsStatus;
import com.example.butex.enums.SubscriptionStatus;
import com.example.butex.repository.SubscriptionRepository;
import com.example.butex.repository.TierDetailsRepository;
import com.example.butex.repository.TierRepository;
import com.example.butex.repository.UserOrderRepository;
import com.example.butex.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TierEvaluationService {

    public static final String CRON_ACTION_BY = "tier-promotion-cron";

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final UserOrderRepository userOrderRepository;
    private final TierRepository tierRepository;
    private final TierDetailsRepository tierDetailsRepository;
    private final SubscriptionService subscriptionService;

    public int evaluateAndPromoteEligibleUsers() {
        List<Subscription> activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
        int promoted = 0;
        for (Subscription subscription : activeSubscriptions) {
            if (evaluateAndPromote(subscription)) {
                promoted++;
            }
        }
        log.info("Tier promotion job finished: promoted {} of {} active subscriptions",
                promoted, activeSubscriptions.size());
        return promoted;
    }

    @Transactional
    public boolean evaluateAndPromote(Subscription subscription) {
        User user = userRepository.findById(subscription.getUserId()).orElse(null);
        if (user == null || !user.isActive()) {
            return false;
        }

        TierDetails currentTierDetails = tierDetailsRepository.findById(subscription.getTierDetailsId()).orElse(null);
        if (currentTierDetails == null) {
            return false;
        }

        Tier currentTier = tierRepository.findById(currentTierDetails.getTierId()).orElse(null);
        if (currentTier == null) {
            return false;
        }

        long totalOrders = userOrderRepository.countByUserId(user.getId());
        BigDecimal monthlySpend = monthlyOrderValue(user.getId());

        TierDetails targetTierDetails = findHighestQualifyingTierDetails(user, totalOrders, monthlySpend);
        if (targetTierDetails == null) {
            return false;
        }

        Tier targetTier = tierRepository.findById(targetTierDetails.getTierId()).orElse(null);
        if (targetTier == null || targetTier.getRank() <= currentTier.getRank()) {
            return false;
        }

        boolean promoted = subscriptionService.promoteToTier(subscription, targetTierDetails, currentTier, targetTier,
                CRON_ACTION_BY);
        if (promoted) {
            log.info("Auto-promoted user {} from {} to {}", user.getId(), currentTier.getCode(), targetTier.getCode());
        }
        return promoted;
    }

    private TierDetails findHighestQualifyingTierDetails(User user, long totalOrders, BigDecimal monthlySpend) {
        TierDetails bestMatch = null;
        int bestRank = -1;

        for (Tier tier : tierRepository.findByActiveTrueOrderByRankAsc()) {
            TierDetails details = tierDetailsRepository
                    .findByTierIdAndStatusAndIsDefaultTrue(tier.getId(), PlanDetailsStatus.ACTIVE)
                    .orElse(null);
            if (details == null) {
                continue;
            }
            if (qualifies(user, details, totalOrders, monthlySpend) && tier.getRank() > bestRank) {
                bestMatch = details;
                bestRank = tier.getRank();
            }
        }
        return bestMatch;
    }

    boolean qualifies(User user, TierDetails tierDetails, long totalOrders, BigDecimal monthlySpend) {
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

    private BigDecimal monthlyOrderValue(Long userId) {
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDateTime monthStart = firstDay.atStartOfDay();
        LocalDateTime monthEnd = firstDay.plusMonths(1).atStartOfDay();
        return userOrderRepository.sumOrderValueForUserBetween(userId, monthStart, monthEnd);
    }
}

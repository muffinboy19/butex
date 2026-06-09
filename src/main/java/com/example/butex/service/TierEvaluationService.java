package com.example.butex.service;

import com.example.butex.entity.Subscription;
import com.example.butex.enums.SubscriptionStatus;
import com.example.butex.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TierEvaluationService {

    private final SubscriptionRepository subscriptionRepository;
    private final TierPromotionJobExecutor tierPromotionJobExecutor;

    public int evaluateAndPromoteEligibleUsers() {
        List<Subscription> activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
        int promoted = 0;
        for (Subscription subscription : activeSubscriptions) {
            if (tierPromotionJobExecutor.promoteIfEligible(subscription)) {
                promoted++;
            }
        }
        log.info("Tier promotion job finished: promoted {} of {} active subscriptions",
                promoted, activeSubscriptions.size());
        return promoted;
    }
}

package com.example.butex.service.scheduler;

import com.example.butex.dto.response.JobTriggerResponse;
import com.example.butex.entity.Subscription;
import com.example.butex.enums.SubscriptionStatus;
import com.example.butex.repository.SubscriptionRepository;
import com.example.butex.service.SubscriptionService;
import com.example.butex.service.TierPromotionJobExecutor;
import com.example.butex.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private static final String SUBSCRIPTION_EXPIRY_JOB = "subscription-expiry";
    private static final String TIER_PROMOTION_JOB = "tier-promotion";

    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final TierPromotionJobExecutor tierPromotionJobExecutor;

    @Scheduled(cron = Constants.SUBSCRIPTION_EXPIRY_CRON)
    public void expireSubscriptions() {
        runSubscriptionExpiryJob();
    }

    @Scheduled(cron = Constants.TIER_PROMOTION_CRON)
    public void promoteEligibleUsers() {
        runTierPromotionJob();
    }

    public JobTriggerResponse runSubscriptionExpiryJob() {
        // Redis distributed lock disabled while Redis is unavailable
        log.info("Starting subscription expiry job");
        int checked = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE).size();
        int expired = subscriptionService.expireOverdueSubscriptions();
        return JobTriggerResponse.builder()
                .job(SUBSCRIPTION_EXPIRY_JOB)
                .checked(checked)
                .affected(expired)
                .build();
    }

    public JobTriggerResponse runTierPromotionJob() {
        // Redis distributed lock disabled while Redis is unavailable
        log.info("Starting tier promotion job");
        return evaluateAndPromoteEligibleUsers();
    }

    private JobTriggerResponse evaluateAndPromoteEligibleUsers() {
        List<Subscription> activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
        int promoted = 0;
        for (Subscription subscription : activeSubscriptions) {
            if (tierPromotionJobExecutor.promoteIfEligible(subscription)) {
                promoted++;
            }
        }
        log.info("Tier promotion job finished: promoted {} of {} active subscriptions",
                promoted, activeSubscriptions.size());
        return JobTriggerResponse.builder()
                .job(TIER_PROMOTION_JOB)
                .checked(activeSubscriptions.size())
                .affected(promoted)
                .build();
    }
}

package com.example.butex.service.scheduler;

import com.example.butex.dto.response.JobTriggerResponse;
import com.example.butex.entity.Subscription;
import com.example.butex.enums.SubscriptionStatus;
import com.example.butex.repository.SubscriptionRepository;
import com.example.butex.service.SubscriptionService;
import com.example.butex.service.TierPromotionJobExecutor;
// import com.example.butex.service.lock.LockService;
import com.example.butex.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

// import java.time.LocalDateTime;
// import java.time.temporal.ChronoUnit;
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
    // private final LockService lockService;

    @Scheduled(cron = Constants.SUBSCRIPTION_EXPIRY_CRON)
    public void expireSubscriptions() {
        runSubscriptionExpiryJob();
    }

    @Scheduled(cron = Constants.TIER_PROMOTION_CRON)
    public void promoteEligibleUsers() {
        runTierPromotionJob();
    }

    public JobTriggerResponse runSubscriptionExpiryJob() {
        // String key = Constants.SUBSCRIPTION_EXPIRY_LOCK_PREFIX + LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        // lockService.getLockOnKey(key);
        // try {
        log.info("Starting subscription expiry job");
        int checked = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE).size();
        int expired = subscriptionService.expireOverdueSubscriptions();
        return JobTriggerResponse.builder()
                .job(SUBSCRIPTION_EXPIRY_JOB)
                .checked(checked)
                .affected(expired)
                .build();
        // } finally {
        //     lockService.releaseLockOnKey(key);
        // }
    }

    public JobTriggerResponse runTierPromotionJob() {
        // String key = Constants.TIER_PROMOTION_LOCK_PREFIX + LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        // lockService.getLockOnKey(key);
        // try {
        log.info("Starting tier promotion job");
        return evaluateAndPromoteEligibleUsers();
        // } finally {
        //     lockService.releaseLockOnKey(key);
        // }
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

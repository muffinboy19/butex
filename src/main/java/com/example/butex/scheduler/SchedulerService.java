package com.example.butex.scheduler;

import com.example.butex.service.SubscriptionExpiryService;
import com.example.butex.service.TierEvaluationService;
import com.example.butex.service.lock.DistributedLockService;
import com.example.butex.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final SubscriptionExpiryService subscriptionExpiryService;
    private final TierEvaluationService tierEvaluationService;
    private final DistributedLockService distributedLockService;

    @Scheduled(cron = Constants.SUBSCRIPTION_EXPIRY_CRON)
    public void expireSubscriptions() {
        String key = Constants.SUBSCRIPTION_EXPIRY_LOCK_PREFIX + LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        distributedLockService.getLockOnKey(key);
        try {
            log.info("Starting scheduled subscription expiry job");
            subscriptionExpiryService.expireOverdueSubscriptions();
        } finally {
            distributedLockService.releaseLockOnKey(key);
        }
    }

    @Scheduled(cron = Constants.TIER_PROMOTION_CRON)
    public void promoteEligibleUsers() {
        String key = Constants.TIER_PROMOTION_LOCK_PREFIX + LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        distributedLockService.getLockOnKey(key);
        try {
            log.info("Starting scheduled tier promotion job");
            tierEvaluationService.evaluateAndPromoteEligibleUsers();
        } finally {
            distributedLockService.releaseLockOnKey(key);
        }
    }
}

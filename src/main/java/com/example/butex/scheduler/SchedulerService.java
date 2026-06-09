package com.example.butex.server.scheduler;

import com.example.butex.service.SubscriptionExpiryService;
import com.example.butex.service.TierEvaluationService;
import com.example.butex.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final SubscriptionExpiryService subscriptionExpiryService;
    private final TierEvaluationService tierEvaluationService;

    @Scheduled(cron = Constants.SUBSCRIPTION_EXPIRY_CRON)
    public void expireSubscriptions() {
        log.info("Starting scheduled subscription expiry job");
        subscriptionExpiryService.expireOverdueSubscriptions();
    }

    @Scheduled(cron = Constants.TIER_PROMOTION_CRON)
    public void promoteEligibleUsers() {
        log.info("Starting scheduled tier promotion job");
        tierEvaluationService.evaluateAndPromoteEligibleUsers();
    }
}

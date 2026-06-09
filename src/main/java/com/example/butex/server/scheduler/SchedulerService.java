package com.example.butex.server.scheduler;

import com.example.butex.service.SubscriptionExpiryService;
import com.example.butex.service.TierEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final SubscriptionExpiryService subscriptionExpiryService;
    private final TierEvaluationService tierEvaluationService;

    @Value("${membership.subscription-expiry.enabled:true}")
    private boolean subscriptionExpiryEnabled;

    @Value("${membership.tier-promotion.enabled:true}")
    private boolean tierPromotionEnabled;

    @Scheduled(cron = "${membership.subscription-expiry.cron}")
    public void expireSubscriptions() {
        if (!subscriptionExpiryEnabled) {
            return;
        }
        log.info("Starting scheduled subscription expiry job");
        subscriptionExpiryService.expireOverdueSubscriptions();
    }

    @Scheduled(cron = "${membership.tier-promotion.cron}")
    public void promoteEligibleUsers() {
        if (!tierPromotionEnabled) {
            return;
        }
        log.info("Starting scheduled tier promotion job");
        tierEvaluationService.evaluateAndPromoteEligibleUsers();
    }
}

package com.example.butex.scheduler;

import com.example.butex.service.TierEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "membership.tier-promotion.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class TierPromotionScheduler {

    private final TierEvaluationService tierEvaluationService;

    @Scheduled(cron = "${membership.tier-promotion.cron}")
    public void promoteEligibleUsers() {
        log.info("Starting scheduled tier promotion job");
        tierEvaluationService.evaluateAndPromoteEligibleUsers();
    }
}

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
public class SubscriptionExpiryService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionExpiryJobExecutor subscriptionExpiryJobExecutor;
    private final SubscriptionValidityService subscriptionValidityService;

    public void expireOverdueSubscriptions() {
        List<Subscription> activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
        int expired = 0;
        for (Subscription subscription : activeSubscriptions) {
            if (subscriptionValidityService.isEffectivelyActive(subscription)) {
                continue;
            }
            if (subscriptionExpiryJobExecutor.expire(subscription)) {
                expired++;
                log.info("Expired subscription id={} for user {}", subscription.getId(), subscription.getUserId());
            }
        }
        log.info("Subscription expiry job finished: expired {} subscriptions", expired);
    }
}

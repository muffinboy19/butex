package com.example.butex.service;

import com.example.butex.entity.Subscription;
import com.example.butex.entity.UserSubscriptionHistory;
import com.example.butex.enums.SubscriptionStatus;
import com.example.butex.enums.UserSubscriptionHistoryAction;
import com.example.butex.repository.SubscriptionRepository;
import com.example.butex.repository.UserSubscriptionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryService {

    public static final String EXPIRY_ACTION_BY = "subscription-expiry-cron";

    private final SubscriptionRepository subscriptionRepository;
    private final UserSubscriptionHistoryRepository historyRepository;

    public int expireOverdueSubscriptions() {
        List<Subscription> overdue = subscriptionRepository.findByStatusAndExpiresAtBefore(
                SubscriptionStatus.ACTIVE, LocalDateTime.now());
        int expired = 0;
        for (Subscription subscription : overdue) {
            if (expireSubscription(subscription)) {
                expired++;
            }
        }
        log.info("Subscription expiry job finished: expired {} of {} overdue subscriptions",
                expired, overdue.size());
        return expired;
    }

    @Transactional
    public boolean expireSubscription(Subscription subscription) {
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);
        historyRepository.save(UserSubscriptionHistory.builder()
                .userId(subscription.getUserId())
                .subscriptionId(subscription.getId())
                .action(UserSubscriptionHistoryAction.EXPIRED)
                .remark("Membership expired")
                .actionBy(EXPIRY_ACTION_BY)
                .build());
        log.info("Expired subscription id={} for user {}", subscription.getId(), subscription.getUserId());
        return true;
    }
}

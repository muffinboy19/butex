package com.example.butex.service;

import com.example.butex.entity.Subscription;
import com.example.butex.entity.UserSubscriptionHistory;
import com.example.butex.enums.SubscriptionStatus;
import com.example.butex.enums.UserSubscriptionHistoryAction;
import com.example.butex.repository.SubscriptionRepository;
import com.example.butex.repository.UserSubscriptionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionExpiryJobExecutor {

    public static final String EXPIRY_ACTION_BY = "subscription-expiry-cron";

    private final SubscriptionRepository subscriptionRepository;
    private final UserSubscriptionHistoryRepository historyRepository;

    @Transactional
    public boolean expire(Subscription subscription) {
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);
        historyRepository.save(UserSubscriptionHistory.builder()
                .userId(subscription.getUserId())
                .subscriptionId(subscription.getId())
                .action(UserSubscriptionHistoryAction.EXPIRED)
                .remark("Membership expired")
                .actionBy(EXPIRY_ACTION_BY)
                .build());
        return true;
    }
}

package com.example.butex.service;

import com.example.butex.entity.Subscription;
import com.example.butex.enums.SubscriptionStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SubscriptionValidityService {

    public boolean isEffectivelyActive(Subscription subscription) {
        return subscription != null
                && subscription.getStatus() == SubscriptionStatus.ACTIVE
                && subscription.getExpiresAt().isAfter(LocalDateTime.now());
    }
}

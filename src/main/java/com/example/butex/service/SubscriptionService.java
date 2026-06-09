package com.example.butex.service;

import com.example.butex.entity.Plan;
import com.example.butex.entity.PlanDetails;
import com.example.butex.entity.Subscription;
import com.example.butex.entity.Tier;
import com.example.butex.entity.TierDetails;
import com.example.butex.entity.UserSubscriptionHistory;
import com.example.butex.enums.SubscriptionStatus;
import com.example.butex.enums.SubscriptionSubStatus;
import com.example.butex.enums.UserSubscriptionHistoryAction;
import com.example.butex.dto.request.ChangeTierRequest;
import com.example.butex.dto.request.SubscribeRequest;
import com.example.butex.dto.response.SubscriptionResponse;
import com.example.butex.exception.BusinessException;
import com.example.butex.exception.ResourceNotFoundException;
import com.example.butex.repository.PlanRepository;
import com.example.butex.repository.SubscriptionRepository;
import com.example.butex.repository.TierRepository;
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
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserSubscriptionHistoryRepository historyRepository;
    private final UserService userService;
    private final MembershipCatalogService catalogService;
    private final PlanRepository planRepository;
    private final TierRepository tierRepository;

    @Transactional
    public synchronized SubscriptionResponse subscribe(Long userId, SubscribeRequest request) {
        userService.findUser(userId);
        validateSubscribeRequest(request);

        if (subscriptionRepository.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)) {
            throw new BusinessException("User already has an active subscription");
        }

        PlanDetails planDetails = catalogService.getActivePlanDetails(request.getPlanDetailsId());
        TierDetails tierDetails = catalogService.getActiveTierDetails(request.getTierDetailsId());

        LocalDateTime startsAt = LocalDateTime.now();
        Subscription subscription = Subscription.builder()
                .userId(userId)
                .planDetailsId(planDetails.getId())
                .tierDetailsId(tierDetails.getId())
                .startsAt(startsAt)
                .expiresAt(startsAt.plusDays(planDetails.getDurationDays()))
                .status(SubscriptionStatus.ACTIVE)
                .subStatus(SubscriptionSubStatus.NEW)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        saveHistory(userId, saved.getId(), UserSubscriptionHistoryAction.CREATED, "Subscribed to membership", "system");
        log.info("User {} subscribed subscriptionId={}", userId, saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getCurrentSubscription(Long userId) {
        userService.findUser(userId);
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription for user: " + userId));
        return toResponse(subscription);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptionHistory(Long userId) {
        userService.findUser(userId);
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public synchronized SubscriptionResponse cancelSubscription(Long userId) {
        Subscription subscription = getActiveSubscriptionOrThrow(userId);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        Subscription saved = subscriptionRepository.save(subscription);
        saveHistory(userId, saved.getId(), UserSubscriptionHistoryAction.CANCELLED, "Subscription cancelled", "system");
        log.info("User {} cancelled subscriptionId={}", userId, saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public synchronized SubscriptionResponse changeTier(Long userId, ChangeTierRequest request) {
        if (request.getTierDetailsId() == null) {
            throw new BusinessException("tierDetailsId is required");
        }

        Subscription subscription = getActiveSubscriptionOrThrow(userId);
        TierDetails currentTierDetails = catalogService.getActiveTierDetails(subscription.getTierDetailsId());
        TierDetails newTierDetails = catalogService.getActiveTierDetails(request.getTierDetailsId());

        Tier currentTier = tierRepository.findById(currentTierDetails.getTierId())
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found"));
        Tier newTier = tierRepository.findById(newTierDetails.getTierId())
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found"));

        if (currentTierDetails.getId().equals(newTierDetails.getId())) {
            throw new BusinessException("User is already on this tier details version");
        }

        UserSubscriptionHistoryAction action;
        if (newTier.getRank() > currentTier.getRank()) {
            action = UserSubscriptionHistoryAction.TIER_UPGRADED;
        } else if (newTier.getRank() < currentTier.getRank()) {
            action = UserSubscriptionHistoryAction.TIER_DOWNGRADED;
        } else {
            action = UserSubscriptionHistoryAction.MODIFIED;
        }

        subscription.setTierDetailsId(newTierDetails.getId());
        Subscription saved = subscriptionRepository.save(subscription);
        saveHistory(userId, saved.getId(), action,
                "Tier changed from " + currentTier.getCode() + " to " + newTier.getCode(), "system");
        log.info("User {} changed tier on subscriptionId={}", userId, saved.getId());
        return toResponse(saved);
    }

    private Subscription getActiveSubscriptionOrThrow(Long userId) {
        userService.findUser(userId);
        return subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription for user: " + userId));
    }

    private void validateSubscribeRequest(SubscribeRequest request) {
        if (request.getPlanDetailsId() == null || request.getTierDetailsId() == null) {
            throw new BusinessException("planDetailsId and tierDetailsId are required");
        }
    }

    private void saveHistory(Long userId, Long subscriptionId, UserSubscriptionHistoryAction action,
                             String remark, String actionBy) {
        UserSubscriptionHistory history = UserSubscriptionHistory.builder()
                .userId(userId)
                .subscriptionId(subscriptionId)
                .action(action)
                .remark(remark)
                .actionBy(actionBy)
                .build();
        historyRepository.save(history);
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        PlanDetails planDetails = catalogService.findPlanDetails(subscription.getPlanDetailsId());
        TierDetails tierDetails = catalogService.findTierDetails(subscription.getTierDetailsId());

        Plan plan = planRepository.findById(planDetails.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        Tier tier = tierRepository.findById(tierDetails.getTierId())
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found"));

        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUserId())
                .planDetailsId(subscription.getPlanDetailsId())
                .tierDetailsId(subscription.getTierDetailsId())
                .planCode(plan.getCode())
                .planName(plan.getName())
                .tierCode(tier.getCode())
                .tierName(tier.getName())
                .startsAt(subscription.getStartsAt())
                .expiresAt(subscription.getExpiresAt())
                .status(subscription.getStatus())
                .subStatus(subscription.getSubStatus())
                .build();
    }
}

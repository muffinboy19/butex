package com.example.butex.service;

import com.example.butex.entity.Plan;
import com.example.butex.entity.PlanDetails;
import com.example.butex.entity.Subscription;
import com.example.butex.entity.Tier;
import com.example.butex.entity.TierDetails;
import com.example.butex.entity.User;
import com.example.butex.entity.UserSubscriptionHistory;
import com.example.butex.enums.SubscriptionStatus;
import com.example.butex.enums.SubscriptionSubStatus;
import com.example.butex.enums.UserSubscriptionHistoryAction;
import com.example.butex.dto.request.ChangePlanRequest;
import com.example.butex.dto.request.ChangeTierRequest;
import com.example.butex.dto.request.SubscribeRequest;
import com.example.butex.dto.response.SubscriptionResponse;
import com.example.butex.dto.response.UserSubscriptionHistoryResponse;
import com.example.butex.exception.BusinessException;
import com.example.butex.exception.ResourceNotFoundException;
import com.example.butex.repository.PlanRepository;
import com.example.butex.repository.SubscriptionRepository;
import com.example.butex.repository.TierRepository;
import com.example.butex.repository.UserOrderRepository;
import com.example.butex.repository.UserSubscriptionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    public static final String EXPIRY_ACTION_BY = "subscription-expiry-cron";

    private final SubscriptionRepository subscriptionRepository;
    private final UserSubscriptionHistoryRepository historyRepository;
    private final UserService userService;
    private final MembershipService membershipService;
    private final PlanRepository planRepository;
    private final TierRepository tierRepository;
    private final UserOrderRepository userOrderRepository;

    public boolean isEffectivelyActive(Subscription subscription) {
        return subscription != null
                && subscription.getStatus() == SubscriptionStatus.ACTIVE
                && subscription.getExpiresAt().isAfter(LocalDateTime.now());
    }

    public void expireOverdueSubscriptions() {
        List<Subscription> activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
        int expired = 0;
        for (Subscription subscription : activeSubscriptions) {
            if (isEffectivelyActive(subscription)) {
                continue;
            }
            if (expire(subscription)) {
                expired++;
                log.info("Expired subscription id={} for user {}", subscription.getId(), subscription.getUserId());
            }
        }
        log.info("Subscription expiry job finished: expired {} subscriptions", expired);
    }

    @Transactional
    public boolean expire(Subscription subscription) {
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);
        saveHistory(subscription.getUserId(), subscription.getId(), UserSubscriptionHistoryAction.EXPIRED,
                "Membership expired", EXPIRY_ACTION_BY);
        return true;
    }

    @Transactional(readOnly = true)
    public void requireEligibleTier(User user, TierDetails tierDetails) {
        if (!qualifiesForTier(user, tierDetails)) {
            Tier tier = tierRepository.findById(tierDetails.getTierId())
                    .orElseThrow(() -> new BusinessException("Tier not found"));
            throw new BusinessException("User does not qualify for tier: " + tier.getCode());
        }
    }

    @Transactional(readOnly = true)
    public boolean qualifiesForTier(User user, TierDetails tierDetails) {
        long totalOrders = userOrderRepository.countByUserId(user.getId());
        BigDecimal monthlySpend = monthlyOrderValue(user.getId());
        return qualifiesForTier(user, tierDetails, totalOrders, monthlySpend);
    }

    public boolean qualifiesForTier(User user, TierDetails tierDetails, long totalOrders, BigDecimal monthlySpend) {
        if (tierDetails.getMinOrders() != null && totalOrders < tierDetails.getMinOrders()) {
            return false;
        }
        if (tierDetails.getMinMonthlyOrderValue() != null
                && monthlySpend.compareTo(tierDetails.getMinMonthlyOrderValue()) < 0) {
            return false;
        }
        if (tierDetails.getCohortCode() != null && !tierDetails.getCohortCode().isBlank()) {
            if (user.getCohortCode() == null
                    || !tierDetails.getCohortCode().equalsIgnoreCase(user.getCohortCode())) {
                return false;
            }
        }
        return true;
    }

    public BigDecimal monthlyOrderValue(Long userId) {
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDateTime monthStart = firstDay.atStartOfDay();
        LocalDateTime monthEnd = firstDay.plusMonths(1).atStartOfDay();
        return userOrderRepository.sumOrderValueForUserBetween(userId, monthStart, monthEnd);
    }

    @Transactional
    public SubscriptionResponse subscribe(Long userId, SubscribeRequest request) {
        var user = userService.findUser(userId);
        validateSubscribeRequest(request);

        subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .filter(this::isEffectivelyActive)
                .ifPresent(sub -> {
                    throw new BusinessException("User already has an active subscription");
                });

        PlanDetails requestedPlan = membershipService.getActivePlanDetails(request.getPlanDetailsId());
        TierDetails requestedTier = membershipService.getActiveTierDetails(request.getTierDetailsId());
        PlanDetails planDetails = membershipService.resolveDefaultActivePlanDetails(requestedPlan.getPlanId());
        TierDetails tierDetails = membershipService.resolveDefaultActiveTierDetails(requestedTier.getTierId());
        requireEligibleTier(user, tierDetails);

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
        return toResponse(getEffectivelyActiveSubscriptionOrThrow(userId));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptionHistory(Long userId) {
        userService.findUser(userId);
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSubscriptionHistoryResponse> getSubscriptionActionHistory(Long userId) {
        userService.findUser(userId);
        return historyRepository.findByUserIdOrderByActionAtDesc(userId).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @Transactional
    public SubscriptionResponse cancelSubscription(Long userId) {
        Subscription subscription = getActiveSubscriptionOrThrow(userId);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        Subscription saved = subscriptionRepository.save(subscription);
        saveHistory(userId, saved.getId(), UserSubscriptionHistoryAction.CANCELLED, "Subscription cancelled", "system");
        log.info("User {} cancelled subscriptionId={}", userId, saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse renewSubscription(Long userId) {
        Subscription subscription = getEffectivelyActiveSubscriptionOrThrow(userId);
        PlanDetails currentPlanDetails = membershipService.getActivePlanDetails(subscription.getPlanDetailsId());
        PlanDetails planDetails = membershipService.resolveDefaultActivePlanDetails(currentPlanDetails.getPlanId());

        TierDetails currentTierDetails = membershipService.getActiveTierDetails(subscription.getTierDetailsId());
        TierDetails tierDetails = membershipService.resolveDefaultActiveTierDetails(currentTierDetails.getTierId());

        boolean planVersionChanged = !planDetails.getId().equals(subscription.getPlanDetailsId());
        boolean tierVersionChanged = !tierDetails.getId().equals(subscription.getTierDetailsId());
        subscription.setPlanDetailsId(planDetails.getId());
        subscription.setTierDetailsId(tierDetails.getId());

        LocalDateTime extensionStart = subscription.getExpiresAt().isAfter(LocalDateTime.now())
                ? subscription.getExpiresAt()
                : LocalDateTime.now();
        subscription.setExpiresAt(extensionStart.plusDays(planDetails.getDurationDays()));
        subscription.setSubStatus(SubscriptionSubStatus.RENEWED);
        subscriptionRepository.save(subscription);

        String remark = "Membership renewed on " + planDetails.getDurationDays() + "-day plan";
        if (planVersionChanged || tierVersionChanged) {
            remark += " (updated to current default plan/tier version)";
        }
        saveHistory(userId, subscription.getId(), UserSubscriptionHistoryAction.RENEWED, remark, "system");
        log.info("User {} renewed subscriptionId={}", userId, subscription.getId());
        return toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse changePlan(Long userId, ChangePlanRequest request) {
        if (request.getNewPlanDetailsId() == null) {
            throw new BusinessException("newPlanDetailsId is required");
        }

        Subscription subscription = getEffectivelyActiveSubscriptionOrThrow(userId);
        PlanDetails currentPlanDetails = membershipService.getActivePlanDetails(subscription.getPlanDetailsId());
        PlanDetails newPlanDetails = membershipService.getActivePlanDetails(request.getNewPlanDetailsId());

        if (currentPlanDetails.getId().equals(newPlanDetails.getId())) {
            throw new BusinessException("User is already on this plan details version");
        }

        Plan currentPlan = planRepository.findById(currentPlanDetails.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        Plan newPlan = planRepository.findById(newPlanDetails.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

        LocalDateTime now = LocalDateTime.now();
        subscription.setPlanDetailsId(newPlanDetails.getId());
        subscription.setStartsAt(now);
        subscription.setExpiresAt(now.plusDays(newPlanDetails.getDurationDays()));
        subscriptionRepository.save(subscription);

        saveHistory(userId, subscription.getId(), UserSubscriptionHistoryAction.PLAN_CHANGED,
                "Plan changed from " + currentPlan.getCode() + " to " + newPlan.getCode(), "system");
        log.info("User {} changed plan on subscriptionId={}", userId, subscription.getId());
        return toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse changeTier(Long userId, ChangeTierRequest request) {
        if (request.getNewTierDetailsId() == null) {
            throw new BusinessException("newTierDetailsId is required");
        }

        var user = userService.findUser(userId);
        Subscription subscription = getEffectivelyActiveSubscriptionOrThrow(userId);
        TierDetails currentTierDetails = membershipService.getActiveTierDetails(subscription.getTierDetailsId());
        TierDetails newTierDetails = membershipService.getActiveTierDetails(request.getNewTierDetailsId());
        requireEligibleTier(user, newTierDetails);

        Tier currentTier = tierRepository.findById(currentTierDetails.getTierId())
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found"));
        Tier newTier = tierRepository.findById(newTierDetails.getTierId())
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found"));

        if (currentTierDetails.getId().equals(newTierDetails.getId())) {
            throw new BusinessException("User is already on this tier details version");
        }

        applyTierChange(subscription, newTierDetails, currentTier, newTier, "system");
        log.info("User {} changed tier on subscriptionId={}", userId, subscription.getId());
        return toResponse(subscription);
    }

    @Transactional
    public boolean promoteToTier(Subscription subscription, TierDetails newTierDetails,
                                 Tier currentTier, Tier newTier, String actionBy) {
        if (newTier.getRank() <= currentTier.getRank()) {
            return false;
        }
        TierDetails currentTierDetails = membershipService.findTierDetails(subscription.getTierDetailsId());
        if (currentTierDetails.getId().equals(newTierDetails.getId())) {
            return false;
        }
        applyTierChange(subscription, newTierDetails, currentTier, newTier, actionBy);
        return true;
    }

    private void applyTierChange(Subscription subscription, TierDetails newTierDetails,
                                 Tier currentTier, Tier newTier, String actionBy) {
        UserSubscriptionHistoryAction action;
        if (newTier.getRank() > currentTier.getRank()) {
            action = UserSubscriptionHistoryAction.TIER_UPGRADED;
        } else if (newTier.getRank() < currentTier.getRank()) {
            action = UserSubscriptionHistoryAction.TIER_DOWNGRADED;
        } else {
            action = UserSubscriptionHistoryAction.MODIFIED;
        }

        subscription.setTierDetailsId(newTierDetails.getId());
        subscriptionRepository.save(subscription);
        saveHistory(subscription.getUserId(), subscription.getId(), action,
                "Tier changed from " + currentTier.getCode() + " to " + newTier.getCode(), actionBy);
    }

    private Subscription getEffectivelyActiveSubscriptionOrThrow(Long userId) {
        Subscription subscription = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription for user: " + userId));
        if (!isEffectivelyActive(subscription)) {
            throw new ResourceNotFoundException("No active subscription for user: " + userId);
        }
        return subscription;
    }

    private Subscription getActiveSubscriptionOrThrow(Long userId) {
        userService.findUser(userId);
        return getEffectivelyActiveSubscriptionOrThrow(userId);
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

    private UserSubscriptionHistoryResponse toHistoryResponse(UserSubscriptionHistory history) {
        return UserSubscriptionHistoryResponse.builder()
                .id(history.getId())
                .userId(history.getUserId())
                .subscriptionId(history.getSubscriptionId())
                .action(history.getAction())
                .remark(history.getRemark())
                .actionBy(history.getActionBy())
                .actionAt(history.getActionAt())
                .build();
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        PlanDetails planDetails = membershipService.findPlanDetails(subscription.getPlanDetailsId());
        TierDetails tierDetails = membershipService.findTierDetails(subscription.getTierDetailsId());

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

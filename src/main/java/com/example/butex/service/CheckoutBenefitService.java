package com.example.butex.service;

import com.example.butex.dto.request.CheckoutBenefitRequest;
import com.example.butex.dto.request.CheckoutLineItemRequest;
import com.example.butex.dto.response.CheckoutBenefitResponse;
import com.example.butex.dto.response.LineItemBenefitResponse;
import com.example.butex.entity.Plan;
import com.example.butex.entity.PlanDetails;
import com.example.butex.entity.PlanDiscountRule;
import com.example.butex.entity.Subscription;
import com.example.butex.entity.Tier;
import com.example.butex.entity.TierDetails;
import com.example.butex.entity.TierDiscountRule;
import com.example.butex.enums.DiscountTargetType;
import com.example.butex.enums.SubscriptionStatus;
import com.example.butex.exception.ResourceNotFoundException;
import com.example.butex.repository.PlanDiscountRuleRepository;
import com.example.butex.repository.PlanRepository;
import com.example.butex.repository.SubscriptionRepository;
import com.example.butex.repository.TierDiscountRuleRepository;
import com.example.butex.repository.TierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckoutBenefitService {

    private final SubscriptionRepository subscriptionRepository;
    private final MembershipService membershipService;
    private final PlanRepository planRepository;
    private final TierRepository tierRepository;
    private final PlanDiscountRuleRepository planDiscountRuleRepository;
    private final TierDiscountRuleRepository tierDiscountRuleRepository;
    private final UserService userService;
    private final SubscriptionValidityService subscriptionValidityService;

    @Transactional(readOnly = true)
    public CheckoutBenefitResponse calculateBenefits(Long userId, CheckoutBenefitRequest request) {
        userService.findUser(userId);

        Subscription subscription = subscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .filter(subscriptionValidityService::isEffectivelyActive)
                .orElse(null);

        if (subscription == null) {
            return noMembershipResponse(request);
        }

        PlanDetails planDetails = membershipService.findPlanDetails(subscription.getPlanDetailsId());
        TierDetails tierDetails = membershipService.findTierDetails(subscription.getTierDetailsId());
        Plan plan = planRepository.findById(planDetails.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        Tier tier = tierRepository.findById(tierDetails.getTierId())
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found"));

        List<PlanDiscountRule> planRules = planDiscountRuleRepository
                .findByPlanDetailsIdAndActiveTrue(planDetails.getId());
        List<TierDiscountRule> tierRules = tierDiscountRuleRepository
                .findByTierDetailsIdAndActiveTrue(tierDetails.getId());

        BigDecimal cartSubtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<LineItemBenefitResponse> lineBenefits = new ArrayList<>();

        for (CheckoutLineItemRequest item : request.getItems()) {
            BigDecimal discountPercent = bestDiscountPercent(
                    planDetails, tierDetails, planRules, tierRules, item.getItemId(), item.getCategoryId());
            BigDecimal discountAmount = percentOf(item.getLineTotal(), discountPercent);
            BigDecimal payable = item.getLineTotal().subtract(discountAmount);

            cartSubtotal = cartSubtotal.add(item.getLineTotal());
            totalDiscount = totalDiscount.add(discountAmount);

            lineBenefits.add(LineItemBenefitResponse.builder()
                    .itemId(item.getItemId())
                    .categoryId(item.getCategoryId())
                    .lineTotal(item.getLineTotal())
                    .appliedDiscountPercent(discountPercent)
                    .discountAmount(discountAmount)
                    .payableAmount(payable)
                    .build());
        }

        return CheckoutBenefitResponse.builder()
                .membershipActive(true)
                .planCode(plan.getCode())
                .tierCode(tier.getCode())
                .freeDelivery(planDetails.isFreeDeliveryEnabled() || tierDetails.isFreeDeliveryEnabled())
                .exclusiveDealsAccess(planDetails.isExclusiveDealsAccess() || tierDetails.isExclusiveDealsAccess())
                .earlySaleAccess(planDetails.isEarlySaleAccess() || tierDetails.isEarlySaleAccess())
                .prioritySupport(planDetails.isPrioritySupport() || tierDetails.isPrioritySupport())
                .cartSubtotal(cartSubtotal)
                .totalDiscountAmount(totalDiscount)
                .finalPayableAmount(cartSubtotal.subtract(totalDiscount))
                .items(lineBenefits)
                .build();
    }

    private CheckoutBenefitResponse noMembershipResponse(CheckoutBenefitRequest request) {
        BigDecimal cartSubtotal = request.getItems().stream()
                .map(CheckoutLineItemRequest::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LineItemBenefitResponse> lineBenefits = request.getItems().stream()
                .map(item -> LineItemBenefitResponse.builder()
                        .itemId(item.getItemId())
                        .categoryId(item.getCategoryId())
                        .lineTotal(item.getLineTotal())
                        .appliedDiscountPercent(BigDecimal.ZERO)
                        .discountAmount(BigDecimal.ZERO)
                        .payableAmount(item.getLineTotal())
                        .build())
                .toList();

        return CheckoutBenefitResponse.builder()
                .membershipActive(false)
                .freeDelivery(false)
                .exclusiveDealsAccess(false)
                .earlySaleAccess(false)
                .prioritySupport(false)
                .cartSubtotal(cartSubtotal)
                .totalDiscountAmount(BigDecimal.ZERO)
                .finalPayableAmount(cartSubtotal)
                .items(lineBenefits)
                .build();
    }

    private BigDecimal bestDiscountPercent(PlanDetails planDetails, TierDetails tierDetails,
                                           List<PlanDiscountRule> planRules, List<TierDiscountRule> tierRules,
                                           String itemId, String categoryId) {
        BigDecimal best = BigDecimal.ZERO;
        best = max(best, planDetails.getExtraDiscountPercent());
        best = max(best, tierDetails.getExtraDiscountPercent());

        for (PlanDiscountRule rule : planRules) {
            if (matches(rule.getTargetType(), rule.getTargetId(), itemId, categoryId)) {
                best = max(best, rule.getDiscountPercent());
            }
        }
        for (TierDiscountRule rule : tierRules) {
            if (matches(rule.getTargetType(), rule.getTargetId(), itemId, categoryId)) {
                best = max(best, rule.getDiscountPercent());
            }
        }
        return best;
    }

    private boolean matches(DiscountTargetType targetType, String targetId, String itemId, String categoryId) {
        return switch (targetType) {
            case ITEM -> targetId.equalsIgnoreCase(itemId);
            case CATEGORY -> targetId.equalsIgnoreCase(categoryId);
        };
    }

    private BigDecimal max(BigDecimal current, BigDecimal candidate) {
        if (candidate == null) {
            return current;
        }
        return current.max(candidate);
    }

    private BigDecimal percentOf(BigDecimal amount, BigDecimal percent) {
        if (percent == null || percent.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}

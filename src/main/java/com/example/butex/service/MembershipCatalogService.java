package com.example.butex.service;

import com.example.butex.entity.Plan;
import com.example.butex.entity.PlanDetails;
import com.example.butex.entity.Tier;
import com.example.butex.entity.TierDetails;
import com.example.butex.enums.PlanDetailsStatus;
import com.example.butex.dto.response.PlanDetailsResponse;
import com.example.butex.dto.response.PlanResponse;
import com.example.butex.dto.response.TierDetailsResponse;
import com.example.butex.dto.response.TierResponse;
import com.example.butex.repository.PlanDetailsRepository;
import com.example.butex.repository.PlanRepository;
import com.example.butex.repository.TierDetailsRepository;
import com.example.butex.repository.TierRepository;
import com.example.butex.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipCatalogService {

    private final PlanRepository planRepository;
    private final PlanDetailsRepository planDetailsRepository;
    private final TierRepository tierRepository;
    private final TierDetailsRepository tierDetailsRepository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = Constants.CACHE_MEMBERSHIP_PLANS)
    public List<PlanResponse> getActivePlans() {
        return planRepository.findByActiveTrue().stream()
                .map(this::toPlanResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TierResponse> getActiveTiers() {
        return tierRepository.findByActiveTrueOrderByRankAsc().stream()
                .map(this::toTierResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanDetails getActivePlanDetails(Long planDetailsId) {
        PlanDetails planDetails = findPlanDetails(planDetailsId);
        if (planDetails.getStatus() != PlanDetailsStatus.ACTIVE || !isEffectiveNow(planDetails)) {
            throw new com.example.butex.exception.BusinessException("Plan details is not active");
        }
        return planDetails;
    }

    @Transactional(readOnly = true)
    public TierDetails getActiveTierDetails(Long tierDetailsId) {
        TierDetails tierDetails = findTierDetails(tierDetailsId);
        if (tierDetails.getStatus() != PlanDetailsStatus.ACTIVE || !isEffectiveNow(tierDetails)) {
            throw new com.example.butex.exception.BusinessException("Tier details is not active");
        }
        return tierDetails;
    }

    public static boolean isEffectiveNow(PlanDetails planDetails) {
        return isWithinEffectiveWindow(planDetails.getEffectiveFrom(), planDetails.getEffectiveTo());
    }

    public static boolean isEffectiveNow(TierDetails tierDetails) {
        return isWithinEffectiveWindow(tierDetails.getEffectiveFrom(), tierDetails.getEffectiveTo());
    }

    private static boolean isWithinEffectiveWindow(LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {
        LocalDateTime now = LocalDateTime.now();
        if (effectiveFrom != null && effectiveFrom.isAfter(now)) {
            return false;
        }
        if (effectiveTo != null && !effectiveTo.isAfter(now)) {
            return false;
        }
        return true;
    }

    @Transactional(readOnly = true)
    public PlanDetails findPlanDetails(Long planDetailsId) {
        return planDetailsRepository.findById(planDetailsId)
                .orElseThrow(() -> new com.example.butex.exception.ResourceNotFoundException(
                        "Plan details not found: " + planDetailsId));
    }

    @Transactional(readOnly = true)
    public TierDetails findTierDetails(Long tierDetailsId) {
        return tierDetailsRepository.findById(tierDetailsId)
                .orElseThrow(() -> new com.example.butex.exception.ResourceNotFoundException(
                        "Tier details not found: " + tierDetailsId));
    }

    private PlanResponse toPlanResponse(Plan plan) {
        List<PlanDetailsResponse> details = planDetailsRepository
                .findByPlanIdAndStatus(plan.getId(), PlanDetailsStatus.ACTIVE)
                .stream()
                .filter(MembershipCatalogService::isEffectiveNow)
                .map(this::toPlanDetailsResponse)
                .toList();

        return PlanResponse.builder()
                .id(plan.getId())
                .code(plan.getCode())
                .name(plan.getName())
                .description(plan.getDescription())
                .activeDetails(details)
                .build();
    }

    private TierResponse toTierResponse(Tier tier) {
        List<TierDetailsResponse> details = tierDetailsRepository
                .findByTierIdAndStatus(tier.getId(), PlanDetailsStatus.ACTIVE)
                .stream()
                .filter(MembershipCatalogService::isEffectiveNow)
                .map(this::toTierDetailsResponse)
                .toList();

        return TierResponse.builder()
                .id(tier.getId())
                .code(tier.getCode())
                .name(tier.getName())
                .description(tier.getDescription())
                .rank(tier.getRank())
                .activeDetails(details)
                .build();
    }

    private PlanDetailsResponse toPlanDetailsResponse(PlanDetails details) {
        return PlanDetailsResponse.builder()
                .id(details.getId())
                .planId(details.getPlanId())
                .version(details.getVersion())
                .durationDays(details.getDurationDays())
                .price(details.getPrice())
                .currency(details.getCurrency())
                .freeDeliveryEnabled(details.isFreeDeliveryEnabled())
                .extraDiscountPercent(details.getExtraDiscountPercent())
                .exclusiveDealsAccess(details.isExclusiveDealsAccess())
                .earlySaleAccess(details.isEarlySaleAccess())
                .prioritySupport(details.isPrioritySupport())
                .isDefault(details.isDefault())
                .build();
    }

    private TierDetailsResponse toTierDetailsResponse(TierDetails details) {
        return TierDetailsResponse.builder()
                .id(details.getId())
                .tierId(details.getTierId())
                .version(details.getVersion())
                .minOrders(details.getMinOrders())
                .minMonthlyOrderValue(details.getMinMonthlyOrderValue())
                .cohortCode(details.getCohortCode())
                .freeDeliveryEnabled(details.isFreeDeliveryEnabled())
                .extraDiscountPercent(details.getExtraDiscountPercent())
                .exclusiveDealsAccess(details.isExclusiveDealsAccess())
                .earlySaleAccess(details.isEarlySaleAccess())
                .prioritySupport(details.isPrioritySupport())
                .isDefault(details.isDefault())
                .build();
    }
}

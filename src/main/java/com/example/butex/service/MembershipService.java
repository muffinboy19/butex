package com.example.butex.service;

import com.example.butex.entity.Plan;
import com.example.butex.entity.PlanDetails;
import com.example.butex.entity.Tier;
import com.example.butex.entity.TierDetails;
import com.example.butex.enums.PlanDetailsStatus;
import com.example.butex.dto.request.CreatePlanDetailsRequest;
import com.example.butex.dto.request.CreateTierDetailsRequest;
import com.example.butex.dto.response.PlanDetailsResponse;
import com.example.butex.dto.response.PlanResponse;
import com.example.butex.dto.response.TierDetailsResponse;
import com.example.butex.dto.response.TierResponse;
import com.example.butex.exception.BusinessException;
import com.example.butex.exception.ResourceNotFoundException;
import com.example.butex.repository.PlanDetailsRepository;
import com.example.butex.repository.PlanRepository;
import com.example.butex.repository.TierDetailsRepository;
import com.example.butex.repository.TierRepository;
import com.example.butex.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipService {

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

    @Transactional
    @CacheEvict(cacheNames = Constants.CACHE_MEMBERSHIP_PLANS, allEntries = true)
    public PlanDetailsResponse createPlanDetails(Long planId, CreatePlanDetailsRequest request) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));
        if (!plan.isActive()) {
            throw new BusinessException("Plan is not active: " + plan.getCode());
        }

        int nextVersion = planDetailsRepository.findMaxVersionByPlanId(planId) + 1;
        LocalDateTime effectiveFrom = request.getEffectiveFrom() != null
                ? request.getEffectiveFrom()
                : LocalDateTime.now();

        PlanDetails saved = planDetailsRepository.save(PlanDetails.builder()
                .planId(planId)
                .version(nextVersion)
                .durationDays(request.getDurationDays())
                .price(request.getPrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .freeDeliveryEnabled(Boolean.TRUE.equals(request.getFreeDeliveryEnabled()))
                .extraDiscountPercent(request.getExtraDiscountPercent())
                .exclusiveDealsAccess(Boolean.TRUE.equals(request.getExclusiveDealsAccess()))
                .earlySaleAccess(Boolean.TRUE.equals(request.getEarlySaleAccess()))
                .prioritySupport(Boolean.TRUE.equals(request.getPrioritySupport()))
                .effectiveFrom(effectiveFrom)
                .effectiveTo(request.getEffectiveTo())
                .status(PlanDetailsStatus.ACTIVE)
                .changeNotes(request.getChangeNotes())
                .build());

        markAsDefaultPlanDetails(planId, saved.getId());
        saved = planDetailsRepository.findById(saved.getId()).orElseThrow();

        log.info("Created plan details id={} for planId={} version={} as default",
                saved.getId(), planId, nextVersion);
        return toPlanDetailsResponse(saved);
    }

    @Transactional
    public TierDetailsResponse createTierDetails(Long tierId, CreateTierDetailsRequest request) {
        Tier tier = tierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found: " + tierId));
        if (!tier.isActive()) {
            throw new BusinessException("Tier is not active: " + tier.getCode());
        }

        int nextVersion = tierDetailsRepository.findMaxVersionByTierId(tierId) + 1;

        TierDetails saved = tierDetailsRepository.save(TierDetails.builder()
                .tierId(tierId)
                .version(nextVersion)
                .minOrders(request.getMinOrders())
                .minMonthlyOrderValue(request.getMinMonthlyOrderValue())
                .cohortCode(request.getCohortCode())
                .freeDeliveryEnabled(Boolean.TRUE.equals(request.getFreeDeliveryEnabled()))
                .extraDiscountPercent(request.getExtraDiscountPercent())
                .exclusiveDealsAccess(Boolean.TRUE.equals(request.getExclusiveDealsAccess()))
                .earlySaleAccess(Boolean.TRUE.equals(request.getEarlySaleAccess()))
                .prioritySupport(Boolean.TRUE.equals(request.getPrioritySupport()))
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .status(PlanDetailsStatus.ACTIVE)
                .changeNotes(request.getChangeNotes())
                .build());

        markAsDefaultTierDetails(tierId, saved.getId());
        saved = tierDetailsRepository.findById(saved.getId()).orElseThrow();

        log.info("Created tier details id={} for tierId={} version={} as default",
                saved.getId(), tierId, nextVersion);
        return toTierDetailsResponse(saved);
    }

    @Transactional(readOnly = true)
    public PlanDetails resolveDefaultActivePlanDetails(Long planId) {
        return planDetailsRepository
                .findByPlanIdAndStatusAndIsDefaultTrue(planId, PlanDetailsStatus.ACTIVE)
                .filter(MembershipService::isEffectiveNow)
                .orElseThrow(() -> new BusinessException("No default active plan details for plan: " + planId));
    }

    @Transactional(readOnly = true)
    public TierDetails resolveDefaultActiveTierDetails(Long tierId) {
        return tierDetailsRepository
                .findByTierIdAndStatusAndIsDefaultTrue(tierId, PlanDetailsStatus.ACTIVE)
                .filter(MembershipService::isEffectiveNow)
                .orElseThrow(() -> new BusinessException("No default active tier details for tier: " + tierId));
    }

    @Transactional(readOnly = true)
    public PlanDetails getActivePlanDetails(Long planDetailsId) {
        PlanDetails planDetails = findPlanDetails(planDetailsId);
        if (planDetails.getStatus() != PlanDetailsStatus.ACTIVE || !isEffectiveNow(planDetails)) {
            throw new BusinessException("Plan details is not active");
        }
        return planDetails;
    }

    @Transactional(readOnly = true)
    public TierDetails getActiveTierDetails(Long tierDetailsId) {
        TierDetails tierDetails = findTierDetails(tierDetailsId);
        if (tierDetails.getStatus() != PlanDetailsStatus.ACTIVE || !isEffectiveNow(tierDetails)) {
            throw new BusinessException("Tier details is not active");
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plan details not found: " + planDetailsId));
    }

    @Transactional(readOnly = true)
    public TierDetails findTierDetails(Long tierDetailsId) {
        return tierDetailsRepository.findById(tierDetailsId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tier details not found: " + tierDetailsId));
    }

    private PlanResponse toPlanResponse(Plan plan) {
        List<PlanDetailsResponse> details = planDetailsRepository
                .findByPlanIdAndStatus(plan.getId(), PlanDetailsStatus.ACTIVE)
                .stream()
                .filter(MembershipService::isEffectiveNow)
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
                .filter(MembershipService::isEffectiveNow)
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

    private void markAsDefaultPlanDetails(Long planId, Long planDetailsId) {
        planDetailsRepository.findByPlanId(planId).forEach(details -> {
            details.setDefault(details.getId().equals(planDetailsId));
            planDetailsRepository.save(details);
        });
    }

    private void markAsDefaultTierDetails(Long tierId, Long tierDetailsId) {
        tierDetailsRepository.findByTierId(tierId).forEach(details -> {
            details.setDefault(details.getId().equals(tierDetailsId));
            tierDetailsRepository.save(details);
        });
    }
}

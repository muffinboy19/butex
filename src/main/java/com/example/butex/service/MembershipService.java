package com.example.butex.service;

import com.example.butex.entity.Plan;
import com.example.butex.entity.PlanDetails;
import com.example.butex.entity.PlanDetailsHistory;
import com.example.butex.entity.Tier;
import com.example.butex.entity.TierDetails;
import com.example.butex.entity.TierDetailsHistory;
import com.example.butex.enums.PlanDetailsStatus;
import com.example.butex.enums.PlanDetailsHistoryAction;
import com.example.butex.enums.TierDetailsHistoryAction;
import com.example.butex.dto.request.CreatePlanDetailsRequest;
import com.example.butex.dto.request.CreateTierDetailsRequest;
import com.example.butex.dto.request.UpdatePlanDetailsRequest;
import com.example.butex.dto.request.UpdateTierDetailsRequest;
import com.example.butex.dto.response.PlanDetailsResponse;
import com.example.butex.dto.response.PlanResponse;
import com.example.butex.dto.response.TierDetailsResponse;
import com.example.butex.dto.response.TierResponse;
import com.example.butex.exception.BusinessException;
import com.example.butex.exception.ResourceNotFoundException;
import com.example.butex.repository.PlanDetailsHistoryRepository;
import com.example.butex.repository.PlanDetailsRepository;
import com.example.butex.repository.PlanRepository;
import com.example.butex.repository.TierDetailsHistoryRepository;
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
    private final PlanDetailsHistoryRepository planDetailsHistoryRepository;
    private final TierRepository tierRepository;
    private final TierDetailsRepository tierDetailsRepository;
    private final TierDetailsHistoryRepository tierDetailsHistoryRepository;

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
        LocalDateTime effectiveFrom = LocalDateTime.now();

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
                .status(PlanDetailsStatus.ACTIVE)
                .changeNotes(request.getChangeNotes())
                .build());

        markAsDefaultPlanDetails(planId, saved.getId());
        saved = planDetailsRepository.findById(saved.getId()).orElseThrow();

        String remark = request.getChangeNotes() != null
                ? request.getChangeNotes()
                : "Created plan details version " + nextVersion;
        savePlanDetailsHistory(saved.getId(), PlanDetailsHistoryAction.CREATED, remark);

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
        LocalDateTime effectiveFrom = LocalDateTime.now();

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
                .effectiveFrom(effectiveFrom)
                .status(PlanDetailsStatus.ACTIVE)
                .changeNotes(request.getChangeNotes())
                .build());

        markAsDefaultTierDetails(tierId, saved.getId());
        saved = tierDetailsRepository.findById(saved.getId()).orElseThrow();

        String remark = request.getChangeNotes() != null
                ? request.getChangeNotes()
                : "Created tier details version " + nextVersion;
        saveTierDetailsHistory(saved.getId(), TierDetailsHistoryAction.CREATED, remark);

        log.info("Created tier details id={} for tierId={} version={} as default",
                saved.getId(), tierId, nextVersion);
        return toTierDetailsResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = Constants.CACHE_MEMBERSHIP_PLANS, allEntries = true)
    public PlanDetailsResponse updatePlanDetails(Long planId, Long detailsId, UpdatePlanDetailsRequest request) {
        planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));

        PlanDetails details = findPlanDetails(detailsId);
        if (!details.getPlanId().equals(planId)) {
            throw new BusinessException("Plan details does not belong to plan: " + planId);
        }

        if (!applyPlanDetailsUpdates(details, request)) {
            throw new BusinessException("No fields to update");
        }

        PlanDetails saved = planDetailsRepository.save(details);
        savePlanDetailsHistory(saved.getId(), PlanDetailsHistoryAction.MODIFIED,
                request.getChangeNotes() != null ? request.getChangeNotes() : "Plan details updated");

        log.info("Updated plan details id={} for planId={} version={}", saved.getId(), planId, saved.getVersion());
        return toPlanDetailsResponse(saved);
    }

    @Transactional
    public TierDetailsResponse updateTierDetails(Long tierId, Long detailsId, UpdateTierDetailsRequest request) {
        tierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found: " + tierId));

        TierDetails details = findTierDetails(detailsId);
        if (!details.getTierId().equals(tierId)) {
            throw new BusinessException("Tier details does not belong to tier: " + tierId);
        }

        if (!applyTierDetailsUpdates(details, request)) {
            throw new BusinessException("No fields to update");
        }

        TierDetails saved = tierDetailsRepository.save(details);
        saveTierDetailsHistory(saved.getId(), TierDetailsHistoryAction.MODIFIED,
                request.getChangeNotes() != null ? request.getChangeNotes() : "Tier details updated");

        log.info("Updated tier details id={} for tierId={} version={}", saved.getId(), tierId, saved.getVersion());
        return toTierDetailsResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = Constants.CACHE_MEMBERSHIP_PLANS, allEntries = true)
    public PlanDetailsResponse deactivatePlanDetails(Long planId, Long detailsId) {
        planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + planId));

        PlanDetails details = findPlanDetails(detailsId);
        if (!details.getPlanId().equals(planId)) {
            throw new BusinessException("Plan details does not belong to plan: " + planId);
        }
        if (details.isDefault()) {
            throw new BusinessException("Cannot deactivate default plan details");
        }
        if (details.getStatus() == PlanDetailsStatus.INACTIVE) {
            throw new BusinessException("Plan details is already inactive");
        }

        details.setStatus(PlanDetailsStatus.INACTIVE);
        PlanDetails saved = planDetailsRepository.save(details);
        savePlanDetailsHistory(saved.getId(), PlanDetailsHistoryAction.DELETED, "Plan details deactivated");

        log.info("Deactivated plan details id={} for planId={} version={}", saved.getId(), planId, saved.getVersion());
        return toPlanDetailsResponse(saved);
    }

    @Transactional
    public TierDetailsResponse deactivateTierDetails(Long tierId, Long detailsId) {
        tierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found: " + tierId));

        TierDetails details = findTierDetails(detailsId);
        if (!details.getTierId().equals(tierId)) {
            throw new BusinessException("Tier details does not belong to tier: " + tierId);
        }
        if (details.isDefault()) {
            throw new BusinessException("Cannot deactivate default tier details");
        }
        if (details.getStatus() == PlanDetailsStatus.INACTIVE) {
            throw new BusinessException("Tier details is already inactive");
        }

        details.setStatus(PlanDetailsStatus.INACTIVE);
        TierDetails saved = tierDetailsRepository.save(details);
        saveTierDetailsHistory(saved.getId(), TierDetailsHistoryAction.DELETED, "Tier details deactivated");

        log.info("Deactivated tier details id={} for tierId={} version={}", saved.getId(), tierId, saved.getVersion());
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

    private boolean applyPlanDetailsUpdates(PlanDetails details, UpdatePlanDetailsRequest request) {
        boolean changed = false;
        if (request.getDurationDays() != null) {
            details.setDurationDays(request.getDurationDays());
            changed = true;
        }
        if (request.getPrice() != null) {
            details.setPrice(request.getPrice());
            changed = true;
        }
        if (request.getCurrency() != null) {
            details.setCurrency(request.getCurrency());
            changed = true;
        }
        if (request.getFreeDeliveryEnabled() != null) {
            details.setFreeDeliveryEnabled(request.getFreeDeliveryEnabled());
            changed = true;
        }
        if (request.getExtraDiscountPercent() != null) {
            details.setExtraDiscountPercent(request.getExtraDiscountPercent());
            changed = true;
        }
        if (request.getExclusiveDealsAccess() != null) {
            details.setExclusiveDealsAccess(request.getExclusiveDealsAccess());
            changed = true;
        }
        if (request.getEarlySaleAccess() != null) {
            details.setEarlySaleAccess(request.getEarlySaleAccess());
            changed = true;
        }
        if (request.getPrioritySupport() != null) {
            details.setPrioritySupport(request.getPrioritySupport());
            changed = true;
        }
        if (request.getEffectiveFrom() != null) {
            details.setEffectiveFrom(request.getEffectiveFrom());
            changed = true;
        }
        if (request.getEffectiveTo() != null) {
            details.setEffectiveTo(request.getEffectiveTo());
            changed = true;
        }
        if (request.getChangeNotes() != null) {
            details.setChangeNotes(request.getChangeNotes());
            changed = true;
        }
        return changed;
    }

    private boolean applyTierDetailsUpdates(TierDetails details, UpdateTierDetailsRequest request) {
        boolean changed = false;
        if (request.getMinOrders() != null) {
            details.setMinOrders(request.getMinOrders());
            changed = true;
        }
        if (request.getMinMonthlyOrderValue() != null) {
            details.setMinMonthlyOrderValue(request.getMinMonthlyOrderValue());
            changed = true;
        }
        if (request.getCohortCode() != null) {
            details.setCohortCode(request.getCohortCode());
            changed = true;
        }
        if (request.getFreeDeliveryEnabled() != null) {
            details.setFreeDeliveryEnabled(request.getFreeDeliveryEnabled());
            changed = true;
        }
        if (request.getExtraDiscountPercent() != null) {
            details.setExtraDiscountPercent(request.getExtraDiscountPercent());
            changed = true;
        }
        if (request.getExclusiveDealsAccess() != null) {
            details.setExclusiveDealsAccess(request.getExclusiveDealsAccess());
            changed = true;
        }
        if (request.getEarlySaleAccess() != null) {
            details.setEarlySaleAccess(request.getEarlySaleAccess());
            changed = true;
        }
        if (request.getPrioritySupport() != null) {
            details.setPrioritySupport(request.getPrioritySupport());
            changed = true;
        }
        if (request.getEffectiveFrom() != null) {
            details.setEffectiveFrom(request.getEffectiveFrom());
            changed = true;
        }
        if (request.getEffectiveTo() != null) {
            details.setEffectiveTo(request.getEffectiveTo());
            changed = true;
        }
        if (request.getChangeNotes() != null) {
            details.setChangeNotes(request.getChangeNotes());
            changed = true;
        }
        return changed;
    }

    private void savePlanDetailsHistory(Long planDetailsId, PlanDetailsHistoryAction action, String remark) {
        planDetailsHistoryRepository.save(PlanDetailsHistory.builder()
                .planDetailsId(planDetailsId)
                .action(action)
                .remark(remark)
                .actionBy("system")
                .build());
    }

    private void saveTierDetailsHistory(Long tierDetailsId, TierDetailsHistoryAction action, String remark) {
        tierDetailsHistoryRepository.save(TierDetailsHistory.builder()
                .tierDetailsId(tierDetailsId)
                .action(action)
                .remark(remark)
                .actionBy("system")
                .build());
    }
}

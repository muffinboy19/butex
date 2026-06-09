package com.example.butex.support;

import com.example.butex.entity.Plan;
import com.example.butex.entity.PlanDetails;
import com.example.butex.entity.Tier;
import com.example.butex.entity.TierDetails;
import com.example.butex.entity.User;
import com.example.butex.enums.PlanDetailsStatus;
import com.example.butex.repository.PlanDetailsRepository;
import com.example.butex.repository.PlanRepository;
import com.example.butex.repository.TierDetailsRepository;
import com.example.butex.repository.TierRepository;
import com.example.butex.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CatalogTestData {

    private final PlanRepository planRepository;
    private final PlanDetailsRepository planDetailsRepository;
    private final TierRepository tierRepository;
    private final TierDetailsRepository tierDetailsRepository;
    private final UserRepository userRepository;

    public SeededCatalog seed() {
        LocalDateTime effectiveFrom = LocalDateTime.now().minusDays(1);

        Plan plan = planRepository.save(Plan.builder()
                .code("MONTHLY")
                .name("Monthly")
                .build());
        PlanDetails planDetails = planDetailsRepository.save(PlanDetails.builder()
                .planId(plan.getId())
                .version(1)
                .durationDays(30)
                .price(new BigDecimal("299.00"))
                .effectiveFrom(effectiveFrom)
                .status(PlanDetailsStatus.ACTIVE)
                .isDefault(true)
                .build());

        Plan yearlyPlan = planRepository.save(Plan.builder()
                .code("YEARLY")
                .name("Yearly")
                .build());
        PlanDetails yearlyPlanDetails = planDetailsRepository.save(PlanDetails.builder()
                .planId(yearlyPlan.getId())
                .version(1)
                .durationDays(365)
                .price(new BigDecimal("2499.00"))
                .effectiveFrom(effectiveFrom)
                .status(PlanDetailsStatus.ACTIVE)
                .isDefault(true)
                .build());

        Tier silver = tierRepository.save(Tier.builder()
                .code("SILVER")
                .name("Silver")
                .rank(1)
                .build());
        TierDetails silverDetails = tierDetailsRepository.save(TierDetails.builder()
                .tierId(silver.getId())
                .version(1)
                .minOrders(0)
                .minMonthlyOrderValue(BigDecimal.ZERO)
                .effectiveFrom(effectiveFrom)
                .status(PlanDetailsStatus.ACTIVE)
                .isDefault(true)
                .build());

        Tier gold = tierRepository.save(Tier.builder()
                .code("GOLD")
                .name("Gold")
                .rank(2)
                .build());
        TierDetails goldDetails = tierDetailsRepository.save(TierDetails.builder()
                .tierId(gold.getId())
                .version(1)
                .minOrders(10)
                .minMonthlyOrderValue(new BigDecimal("5000.00"))
                .effectiveFrom(effectiveFrom)
                .status(PlanDetailsStatus.ACTIVE)
                .isDefault(true)
                .build());

        User user = userRepository.save(User.builder()
                .name("Test User")
                .email("test@example.com")
                .phone("9999999999")
                .build());

        return new SeededCatalog(
                planDetails.getId(),
                yearlyPlanDetails.getId(),
                silverDetails.getId(),
                goldDetails.getId(),
                user.getId());
    }

    @Getter
    @RequiredArgsConstructor
    public static class SeededCatalog {
        private final Long planDetailsId;
        private final Long alternatePlanDetailsId;
        private final Long silverTierDetailsId;
        private final Long goldTierDetailsId;
        private final Long userId;
    }
}

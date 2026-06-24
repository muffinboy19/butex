package com.example.butex.config;

import com.example.butex.entity.Plan;
import com.example.butex.entity.PlanDetails;
import com.example.butex.entity.Tier;
import com.example.butex.entity.TierDetails;
import com.example.butex.enums.PlanDetailsStatus;
import com.example.butex.repository.PlanRepository;
import com.example.butex.repository.PlanDetailsRepository;
import com.example.butex.repository.TierDetailsRepository;
import com.example.butex.repository.TierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class LocalCatalogDataLoader implements ApplicationRunner {

    private final PlanRepository planRepository;
    private final PlanDetailsRepository planDetailsRepository;
    private final TierRepository tierRepository;
    private final TierDetailsRepository tierDetailsRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (planRepository.count() > 0) {
            log.info("Catalog already seeded ({} plans), skipping local data load", planRepository.count());
            return;
        }

        LocalDateTime effectiveFrom = LocalDateTime.now().minusDays(1);
        log.info("Seeding local catalog data...");

        seedPlan("MONTHLY", "Monthly Membership", "Flexible monthly plan", 30, "299.00", false, "5.00");
        seedPlan("QUARTERLY", "Quarterly Membership", "3-month commitment", 90, "799.00", true, "10.00");
        seedPlan("YEARLY", "Yearly Membership", "Best value annual plan", 365, "2499.00", true, "15.00");

        seedTier("SILVER", "Silver", "Entry tier", 1, 0, BigDecimal.ZERO, "5.00");
        seedTier("GOLD", "Gold", "Regular shoppers", 2, 10, new BigDecimal("5000.00"), "10.00");
        seedTier("PLATINUM", "Platinum", "Top tier", 3, 25, new BigDecimal("15000.00"), "15.00");

        log.info("Local catalog seed complete");
    }

    private void seedPlan(String code, String name, String description, int days, String price,
                          boolean freeDelivery, String extraDiscount) {
        Plan plan = planRepository.save(Plan.builder()
                .code(code)
                .name(name)
                .description(description)
                .active(true)
                .build());
        planDetailsRepository.save(PlanDetails.builder()
                .planId(plan.getId())
                .version(1)
                .durationDays(days)
                .price(new BigDecimal(price))
                .currency("INR")
                .freeDeliveryEnabled(freeDelivery)
                .extraDiscountPercent(new BigDecimal(extraDiscount))
                .effectiveFrom(LocalDateTime.now().minusDays(1))
                .status(PlanDetailsStatus.ACTIVE)
                .isDefault(true)
                .build());
    }

    private void seedTier(String code, String name, String description, int rank, int minOrders,
                          BigDecimal minMonthly, String extraDiscount) {
        Tier tier = tierRepository.save(Tier.builder()
                .code(code)
                .name(name)
                .description(description)
                .rank(rank)
                .active(true)
                .build());
        tierDetailsRepository.save(TierDetails.builder()
                .tierId(tier.getId())
                .version(1)
                .minOrders(minOrders)
                .minMonthlyOrderValue(minMonthly)
                .effectiveFrom(LocalDateTime.now().minusDays(1))
                .extraDiscountPercent(new BigDecimal(extraDiscount))
                .status(PlanDetailsStatus.ACTIVE)
                .isDefault(true)
                .build());
    }
}

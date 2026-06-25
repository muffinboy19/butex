package com.example.butex.config;

import com.example.butex.entity.Plan;
import com.example.butex.entity.PlanDetails;
import com.example.butex.entity.PlanDetailsHistory;
import com.example.butex.entity.PlanDiscountRule;
import com.example.butex.entity.Subscription;
import com.example.butex.entity.Tier;
import com.example.butex.entity.TierDetails;
import com.example.butex.entity.TierDetailsHistory;
import com.example.butex.entity.TierDiscountRule;
import com.example.butex.entity.User;
import com.example.butex.entity.UserOrder;
import com.example.butex.entity.UserSubscriptionHistory;
import com.example.butex.enums.DiscountTargetType;
import com.example.butex.enums.PlanDetailsHistoryAction;
import com.example.butex.enums.PlanDetailsStatus;
import com.example.butex.enums.SubscriptionStatus;
import com.example.butex.enums.SubscriptionSubStatus;
import com.example.butex.enums.TierDetailsHistoryAction;
import com.example.butex.enums.UserSubscriptionHistoryAction;
import com.example.butex.repository.PlanDetailsHistoryRepository;
import com.example.butex.repository.PlanDetailsRepository;
import com.example.butex.repository.PlanDiscountRuleRepository;
import com.example.butex.repository.PlanRepository;
import com.example.butex.repository.SubscriptionRepository;
import com.example.butex.repository.TierDetailsHistoryRepository;
import com.example.butex.repository.TierDetailsRepository;
import com.example.butex.repository.TierDiscountRuleRepository;
import com.example.butex.repository.TierRepository;
import com.example.butex.repository.UserOrderRepository;
import com.example.butex.repository.UserRepository;
import com.example.butex.repository.UserSubscriptionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements ApplicationRunner {

    private static final String SEED_EMAIL_DOMAIN = "@butex.local";
    private static final String SEED_EMAIL_PREFIX = "seed.user";
    private static final String SEED_ACTION_BY = "database-seeder";

    private final PlanRepository planRepository;
    private final PlanDetailsRepository planDetailsRepository;
    private final PlanDetailsHistoryRepository planDetailsHistoryRepository;
    private final PlanDiscountRuleRepository planDiscountRuleRepository;
    private final TierRepository tierRepository;
    private final TierDetailsRepository tierDetailsRepository;
    private final TierDetailsHistoryRepository tierDetailsHistoryRepository;
    private final TierDiscountRuleRepository tierDiscountRuleRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserSubscriptionHistoryRepository userSubscriptionHistoryRepository;
    private final UserOrderRepository userOrderRepository;

    @Value("${butex.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${butex.seed.user-count:25}")
    private int seedUserCount;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            log.info("Database seeding disabled (butex.seed.enabled=false)");
            return;
        }
        seedAll();
    }

    @Transactional
    public void seedAll() {
        LocalDateTime effectiveFrom = LocalDateTime.now().minusDays(1);
        seedPlans(effectiveFrom);
        seedTiers(effectiveFrom);
        seedUsersAndRelatedData();
        log.info("Database seed check complete");
    }

    private void seedPlans(LocalDateTime effectiveFrom) {
        seedPlan("MONTHLY", "Monthly Membership",
                "Flexible monthly plan for FirstClub members",
                30, new BigDecimal("299.00"), new BigDecimal("5.00"),
                false, false, false, false, effectiveFrom,
                List.of(
                        rule(DiscountTargetType.CATEGORY, "GROCERIES", "3.00"),
                        rule(DiscountTargetType.ITEM, "SKU-DAIRY-001", "4.00")
                ));

        seedPlan("QUARTERLY", "Quarterly Membership",
                "Save more with a 90-day membership",
                90, new BigDecimal("799.00"), new BigDecimal("7.00"),
                true, false, false, false, effectiveFrom,
                List.of(
                        rule(DiscountTargetType.CATEGORY, "GROCERIES", "5.00"),
                        rule(DiscountTargetType.CATEGORY, "ELECTRONICS", "3.00")
                ));

        seedPlan("YEARLY", "Yearly Membership",
                "Best value annual membership",
                365, new BigDecimal("2499.00"), new BigDecimal("10.00"),
                true, true, true, true, effectiveFrom,
                List.of(
                        rule(DiscountTargetType.CATEGORY, "GROCERIES", "8.00"),
                        rule(DiscountTargetType.CATEGORY, "ELECTRONICS", "5.00"),
                        rule(DiscountTargetType.ITEM, "SKU-PHONE-200", "6.00")
                ));
    }

    private void seedPlan(String code, String name, String description, int durationDays,
                          BigDecimal price, BigDecimal extraDiscountPercent,
                          boolean freeDelivery, boolean exclusiveDeals,
                          boolean earlySale, boolean prioritySupport,
                          LocalDateTime effectiveFrom, List<DiscountRuleSeed> discountRules) {
        Plan plan = planRepository.findByCode(code).orElseGet(() -> {
            log.info("Seeding plan: {}", code);
            return planRepository.save(Plan.builder()
                    .code(code)
                    .name(name)
                    .description(description)
                    .build());
        });

        Optional<PlanDetails> existingDefault = planDetailsRepository
                .findByPlanIdAndStatusAndIsDefaultTrue(plan.getId(), PlanDetailsStatus.ACTIVE);
        if (existingDefault.isPresent()) {
            seedPlanDiscountRules(existingDefault.get().getId(), discountRules);
            return;
        }

        PlanDetails details = planDetailsRepository.save(PlanDetails.builder()
                .planId(plan.getId())
                .version(1)
                .durationDays(durationDays)
                .price(price)
                .extraDiscountPercent(extraDiscountPercent)
                .freeDeliveryEnabled(freeDelivery)
                .exclusiveDealsAccess(exclusiveDeals)
                .earlySaleAccess(earlySale)
                .prioritySupport(prioritySupport)
                .effectiveFrom(effectiveFrom)
                .status(PlanDetailsStatus.ACTIVE)
                .isDefault(true)
                .changeNotes("Initial seed data")
                .build());

        if (planDetailsHistoryRepository.findByPlanDetailsIdOrderByActionAtDesc(details.getId()).isEmpty()) {
            planDetailsHistoryRepository.save(PlanDetailsHistory.builder()
                    .planDetailsId(details.getId())
                    .action(PlanDetailsHistoryAction.CREATED)
                    .remark("Initial seed data")
                    .actionBy(SEED_ACTION_BY)
                    .build());
        }

        seedPlanDiscountRules(details.getId(), discountRules);
        log.info("Seeded plan details for {} (id={})", code, details.getId());
    }

    private void seedPlanDiscountRules(Long planDetailsId, List<DiscountRuleSeed> rules) {
        if (!planDiscountRuleRepository.findByPlanDetailsIdAndActiveTrue(planDetailsId).isEmpty()) {
            return;
        }
        for (DiscountRuleSeed rule : rules) {
            planDiscountRuleRepository.save(PlanDiscountRule.builder()
                    .planDetailsId(planDetailsId)
                    .targetType(rule.targetType())
                    .targetId(rule.targetId())
                    .discountPercent(rule.discountPercent())
                    .build());
        }
    }

    private void seedTiers(LocalDateTime effectiveFrom) {
        seedTier("SILVER", "Silver", "Entry tier for new members", 1,
                0, BigDecimal.ZERO, null,
                new BigDecimal("5.00"), false, false, false, false,
                effectiveFrom,
                List.of(rule(DiscountTargetType.CATEGORY, "GROCERIES", "5.00")));

        seedTier("GOLD", "Gold", "For frequent shoppers", 2,
                10, new BigDecimal("5000.00"), null,
                new BigDecimal("8.00"), true, true, false, false,
                effectiveFrom,
                List.of(
                        rule(DiscountTargetType.CATEGORY, "GROCERIES", "7.00"),
                        rule(DiscountTargetType.CATEGORY, "ELECTRONICS", "4.00")
                ));

        seedTier("PLATINUM", "Platinum", "VIP cohort with premium perks", 3,
                25, new BigDecimal("15000.00"), "VIP",
                new BigDecimal("12.00"), true, true, true, true,
                effectiveFrom,
                List.of(
                        rule(DiscountTargetType.CATEGORY, "GROCERIES", "10.00"),
                        rule(DiscountTargetType.ITEM, "SKU-PHONE-200", "8.00")
                ));
    }

    private void seedTier(String code, String name, String description, int rank,
                          int minOrders, BigDecimal minMonthlyOrderValue, String cohortCode,
                          BigDecimal extraDiscountPercent,
                          boolean freeDelivery, boolean exclusiveDeals,
                          boolean earlySale, boolean prioritySupport,
                          LocalDateTime effectiveFrom, List<DiscountRuleSeed> discountRules) {
        Tier tier = tierRepository.findByCode(code).orElseGet(() -> {
            log.info("Seeding tier: {}", code);
            return tierRepository.save(Tier.builder()
                    .code(code)
                    .name(name)
                    .description(description)
                    .rank(rank)
                    .build());
        });

        Optional<TierDetails> existingDefault = tierDetailsRepository
                .findByTierIdAndStatusAndIsDefaultTrue(tier.getId(), PlanDetailsStatus.ACTIVE);
        if (existingDefault.isPresent()) {
            seedTierDiscountRules(existingDefault.get().getId(), discountRules);
            return;
        }

        TierDetails details = tierDetailsRepository.save(TierDetails.builder()
                .tierId(tier.getId())
                .version(1)
                .minOrders(minOrders)
                .minMonthlyOrderValue(minMonthlyOrderValue)
                .cohortCode(cohortCode)
                .extraDiscountPercent(extraDiscountPercent)
                .freeDeliveryEnabled(freeDelivery)
                .exclusiveDealsAccess(exclusiveDeals)
                .earlySaleAccess(earlySale)
                .prioritySupport(prioritySupport)
                .effectiveFrom(effectiveFrom)
                .status(PlanDetailsStatus.ACTIVE)
                .isDefault(true)
                .changeNotes("Initial seed data")
                .build());

        if (tierDetailsHistoryRepository.findByTierDetailsIdOrderByActionAtDesc(details.getId()).isEmpty()) {
            tierDetailsHistoryRepository.save(TierDetailsHistory.builder()
                    .tierDetailsId(details.getId())
                    .action(TierDetailsHistoryAction.CREATED)
                    .remark("Initial seed data")
                    .actionBy(SEED_ACTION_BY)
                    .build());
        }

        seedTierDiscountRules(details.getId(), discountRules);
        log.info("Seeded tier details for {} (id={})", code, details.getId());
    }

    private void seedTierDiscountRules(Long tierDetailsId, List<DiscountRuleSeed> rules) {
        if (!tierDiscountRuleRepository.findByTierDetailsIdAndActiveTrue(tierDetailsId).isEmpty()) {
            return;
        }
        for (DiscountRuleSeed rule : rules) {
            tierDiscountRuleRepository.save(TierDiscountRule.builder()
                    .tierDetailsId(tierDetailsId)
                    .targetType(rule.targetType())
                    .targetId(rule.targetId())
                    .discountPercent(rule.discountPercent())
                    .build());
        }
    }

    private void seedUsersAndRelatedData() {
        PlanDetails monthlyDetails = defaultPlanDetails("MONTHLY");
        PlanDetails yearlyDetails = defaultPlanDetails("YEARLY");
        TierDetails silverDetails = defaultTierDetails("SILVER");
        TierDetails goldDetails = defaultTierDetails("GOLD");
        TierDetails platinumDetails = defaultTierDetails("PLATINUM");

        List<User> users = new ArrayList<>();
        for (int i = 1; i <= seedUserCount; i++) {
            final int index = i;
            String email = SEED_EMAIL_PREFIX + index + SEED_EMAIL_DOMAIN;
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                String cohortCode = (index >= 16 && index <= 20) ? "VIP" : null;
                return userRepository.save(User.builder()
                        .name("Seed User " + index)
                        .email(email)
                        .phone(String.format("91%08d", index))
                        .cohortCode(cohortCode)
                        .address("Seed Address " + index + ", Bengaluru")
                        .build());
            });
            users.add(user);
        }
        log.info("Ensured {} dummy seed users exist", users.size());

        if (hasSeedSubscriptionData(users.get(0))) {
            log.info("Seed subscriptions/orders already present, skipping");
            return;
        }

        seedActiveSubscription(users.get(0), monthlyDetails, silverDetails,
                SubscriptionSubStatus.NEW, "Seed active monthly + silver subscription");
        seedActiveSubscription(users.get(1), yearlyDetails, silverDetails,
                SubscriptionSubStatus.RENEWED, "Seed active yearly + silver subscription");
        seedActiveSubscription(users.get(2), monthlyDetails, goldDetails,
                SubscriptionSubStatus.NEW, "Seed active monthly + gold subscription");

        seedHistoricalSubscription(users.get(3), monthlyDetails, silverDetails,
                SubscriptionStatus.CANCELLED, UserSubscriptionHistoryAction.CANCELLED,
                "Seed cancelled subscription");
        seedHistoricalSubscription(users.get(4), monthlyDetails, silverDetails,
                SubscriptionStatus.EXPIRED, UserSubscriptionHistoryAction.EXPIRED,
                "Seed expired subscription");

        for (int i = 5; i <= 9; i++) {
            seedOrdersForGoldQualification(users.get(i));
        }

        for (int i = 15; i <= 19; i++) {
            seedActiveSubscription(users.get(i), monthlyDetails, platinumDetails,
                    SubscriptionSubStatus.NEW, "Seed VIP platinum subscription");
        }
    }

    private boolean hasSeedSubscriptionData(User firstSeedUser) {
        return !subscriptionRepository.findByUserIdOrderByCreatedAtDesc(firstSeedUser.getId()).isEmpty();
    }

    private void seedActiveSubscription(User user, PlanDetails planDetails, TierDetails tierDetails,
                                        SubscriptionSubStatus subStatus, String remark) {
        if (subscriptionRepository.findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE).isPresent()) {
            return;
        }
        LocalDateTime startsAt = LocalDateTime.now().minusDays(5);
        LocalDateTime expiresAt = startsAt.plusDays(planDetails.getDurationDays());
        Subscription subscription = subscriptionRepository.save(Subscription.builder()
                .userId(user.getId())
                .planDetailsId(planDetails.getId())
                .tierDetailsId(tierDetails.getId())
                .startsAt(startsAt)
                .expiresAt(expiresAt)
                .status(SubscriptionStatus.ACTIVE)
                .subStatus(subStatus)
                .build());
        userSubscriptionHistoryRepository.save(UserSubscriptionHistory.builder()
                .userId(user.getId())
                .subscriptionId(subscription.getId())
                .action(UserSubscriptionHistoryAction.CREATED)
                .remark(remark)
                .actionBy(SEED_ACTION_BY)
                .build());
    }

    private void seedHistoricalSubscription(User user, PlanDetails planDetails, TierDetails tierDetails,
                                            SubscriptionStatus finalStatus,
                                            UserSubscriptionHistoryAction finalAction, String remark) {
        if (!subscriptionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).isEmpty()) {
            return;
        }
        LocalDateTime startsAt = LocalDateTime.now().minusDays(60);
        LocalDateTime expiresAt = startsAt.plusDays(planDetails.getDurationDays());
        Subscription subscription = subscriptionRepository.save(Subscription.builder()
                .userId(user.getId())
                .planDetailsId(planDetails.getId())
                .tierDetailsId(tierDetails.getId())
                .startsAt(startsAt)
                .expiresAt(expiresAt)
                .status(finalStatus)
                .subStatus(SubscriptionSubStatus.NEW)
                .build());
        userSubscriptionHistoryRepository.save(UserSubscriptionHistory.builder()
                .userId(user.getId())
                .subscriptionId(subscription.getId())
                .action(UserSubscriptionHistoryAction.CREATED)
                .remark(remark)
                .actionBy(SEED_ACTION_BY)
                .build());
        userSubscriptionHistoryRepository.save(UserSubscriptionHistory.builder()
                .userId(user.getId())
                .subscriptionId(subscription.getId())
                .action(finalAction)
                .remark(remark)
                .actionBy(SEED_ACTION_BY)
                .build());
    }

    private void seedOrdersForGoldQualification(User user) {
        if (userOrderRepository.countByUserId(user.getId()) > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (int order = 1; order <= 12; order++) {
            userOrderRepository.save(UserOrder.builder()
                    .userId(user.getId())
                    .orderValue(new BigDecimal("600.00"))
                    .orderedAt(now.minusDays(order))
                    .build());
        }
    }

    private PlanDetails defaultPlanDetails(String planCode) {
        Plan plan = planRepository.findByCode(planCode)
                .orElseThrow(() -> new IllegalStateException("Missing seeded plan: " + planCode));
        return planDetailsRepository
                .findByPlanIdAndStatusAndIsDefaultTrue(plan.getId(), PlanDetailsStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("Missing default plan details: " + planCode));
    }

    private TierDetails defaultTierDetails(String tierCode) {
        Tier tier = tierRepository.findByCode(tierCode)
                .orElseThrow(() -> new IllegalStateException("Missing seeded tier: " + tierCode));
        return tierDetailsRepository
                .findByTierIdAndStatusAndIsDefaultTrue(tier.getId(), PlanDetailsStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("Missing default tier details: " + tierCode));
    }

    private static DiscountRuleSeed rule(DiscountTargetType targetType, String targetId, String percent) {
        return new DiscountRuleSeed(targetType, targetId, new BigDecimal(percent));
    }

    private record DiscountRuleSeed(DiscountTargetType targetType, String targetId, BigDecimal discountPercent) {
    }
}

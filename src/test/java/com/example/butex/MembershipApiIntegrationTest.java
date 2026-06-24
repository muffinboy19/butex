package com.example.butex;

import com.example.butex.config.TestRedisConfig;
import com.example.butex.entity.PlanDetails;
import com.example.butex.entity.Subscription;
import com.example.butex.enums.PlanDetailsHistoryAction;
import com.example.butex.enums.PlanDetailsStatus;
import com.example.butex.enums.SubscriptionStatus;
import com.example.butex.enums.TierDetailsHistoryAction;
import com.example.butex.repository.PlanDetailsHistoryRepository;
import com.example.butex.repository.PlanDetailsRepository;
import com.example.butex.repository.SubscriptionRepository;
import com.example.butex.repository.TierDetailsHistoryRepository;
import com.example.butex.repository.TierDetailsRepository;
import com.example.butex.support.CatalogTestData;
import com.example.butex.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
// import org.springframework.cache.Cache;
// import org.springframework.cache.CacheManager;
// import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestRedisConfig.class)
class MembershipApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CatalogTestData catalogTestData;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanDetailsRepository planDetailsRepository;

    @Autowired
    private PlanDetailsHistoryRepository planDetailsHistoryRepository;

    @Autowired
    private TierDetailsRepository tierDetailsRepository;

    @Autowired
    private TierDetailsHistoryRepository tierDetailsHistoryRepository;

    // @Autowired
    // private CacheManager cacheManager;

    private CatalogTestData.SeededCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = catalogTestData.seed();
    }

    @Test
    void createPlanDetailsAddsNewVersion() throws Exception {
        Long planId = planDetailsRepository.findById(catalog.getPlanDetailsId()).orElseThrow().getPlanId();

        mockMvc.perform(post("/api/v1/membership/plans/{planId}/details", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "durationDays": 30,
                                  "price": 349.00,
                                  "freeDeliveryEnabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.price").value(349.00))
                .andExpect(jsonPath("$.data.freeDeliveryEnabled").value(true))
                .andExpect(jsonPath("$.data.default").value(true));

        Long newDetailsId = planDetailsRepository.findByPlanId(planId).stream()
                .filter(d -> d.getVersion() == 2)
                .findFirst()
                .orElseThrow()
                .getId();
        var history = planDetailsHistoryRepository.findByPlanDetailsIdOrderByActionAtDesc(newDetailsId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAction()).isEqualTo(PlanDetailsHistoryAction.CREATED);

        PlanDetails previousVersion = planDetailsRepository.findById(catalog.getPlanDetailsId()).orElseThrow();
        assertThat(previousVersion.isDefault()).isFalse();
    }

    @Test
    void updatePlanDetailsChangesFieldsAndRecordsHistory() throws Exception {
        Long planId = planDetailsRepository.findById(catalog.getPlanDetailsId()).orElseThrow().getPlanId();

        mockMvc.perform(put("/api/v1/membership/plans/{planId}/details/{detailsId}", planId, catalog.getPlanDetailsId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "extraDiscountPercent": 10.0,
                                  "changeNotes": "Corrected discount from 5% to 10%"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.extraDiscountPercent").value(10.0))
                .andExpect(jsonPath("$.data.version").value(1));

        PlanDetails updated = planDetailsRepository.findById(catalog.getPlanDetailsId()).orElseThrow();
        assertThat(updated.getExtraDiscountPercent()).isEqualByComparingTo("10.0");

        var history = planDetailsHistoryRepository.findByPlanDetailsIdOrderByActionAtDesc(catalog.getPlanDetailsId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAction()).isEqualTo(PlanDetailsHistoryAction.MODIFIED);
        assertThat(history.get(0).getRemark()).isEqualTo("Corrected discount from 5% to 10%");
    }

    @Test
    void updatePlanDetailsWorksForNonDefaultVersion() throws Exception {
        Long planId = planDetailsRepository.findById(catalog.getPlanDetailsId()).orElseThrow().getPlanId();

        mockMvc.perform(post("/api/v1/membership/plans/{planId}/details", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"durationDays": 30, "price": 349.00}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/membership/plans/{planId}/details/{detailsId}", planId, catalog.getPlanDetailsId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price": 279.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.price").value(279.00))
                .andExpect(jsonPath("$.data.default").value(false));
    }

    @Test
    void updateTierDetailsChangesFieldsAndRecordsHistory() throws Exception {
        Long tierId = tierDetailsRepository.findById(catalog.getSilverTierDetailsId()).orElseThrow().getTierId();

        mockMvc.perform(put("/api/v1/membership/tiers/{tierId}/details/{detailsId}",
                        tierId, catalog.getSilverTierDetailsId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "extraDiscountPercent": 8.0,
                                  "changeNotes": "Tier discount hotfix"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.extraDiscountPercent").value(8.0));

        var history = tierDetailsHistoryRepository
                .findByTierDetailsIdOrderByActionAtDesc(catalog.getSilverTierDetailsId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAction()).isEqualTo(TierDetailsHistoryAction.MODIFIED);
    }

    @Test
    void deactivatePlanDetailsSetsInactiveAndRecordsHistory() throws Exception {
        Long planId = planDetailsRepository.findById(catalog.getPlanDetailsId()).orElseThrow().getPlanId();

        mockMvc.perform(post("/api/v1/membership/plans/{planId}/details", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"durationDays": 30, "price": 349.00}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/membership/plans/{planId}/details/{detailsId}",
                        planId, catalog.getPlanDetailsId()))
                .andExpect(status().isOk());

        PlanDetails deactivated = planDetailsRepository.findById(catalog.getPlanDetailsId()).orElseThrow();
        assertThat(deactivated.getStatus()).isEqualTo(PlanDetailsStatus.INACTIVE);

        var history = planDetailsHistoryRepository.findByPlanDetailsIdOrderByActionAtDesc(catalog.getPlanDetailsId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAction()).isEqualTo(PlanDetailsHistoryAction.DELETED);
    }

    @Test
    void deactivateDefaultPlanDetailsReturnsError() throws Exception {
        Long planId = planDetailsRepository.findById(catalog.getPlanDetailsId()).orElseThrow().getPlanId();

        mockMvc.perform(delete("/api/v1/membership/plans/{planId}/details/{detailsId}",
                        planId, catalog.getPlanDetailsId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot deactivate default plan details"));
    }

    @Test
    void deactivateDefaultTierDetailsReturnsError() throws Exception {
        Long tierId = tierDetailsRepository.findById(catalog.getSilverTierDetailsId()).orElseThrow().getTierId();

        mockMvc.perform(delete("/api/v1/membership/tiers/{tierId}/details/{detailsId}",
                        tierId, catalog.getSilverTierDetailsId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot deactivate default tier details"));
    }

    @Test
    void createTierDetailsAddsNewVersion() throws Exception {
        Long tierId = tierDetailsRepository.findById(catalog.getSilverTierDetailsId()).orElseThrow().getTierId();

        mockMvc.perform(post("/api/v1/membership/tiers/{tierId}/details", tierId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "minOrders": 0,
                                  "minMonthlyOrderValue": 0,
                                  "extraDiscountPercent": 7.5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.extraDiscountPercent").value(7.5))
                .andExpect(jsonPath("$.data.default").value(true));
    }

    @Test
    void renewSubscriptionRollsToDefaultPlanVersion() throws Exception {
        saveActiveSubscription();
        Long planId = planDetailsRepository.findById(catalog.getPlanDetailsId()).orElseThrow().getPlanId();

        mockMvc.perform(post("/api/v1/membership/plans/{planId}/details", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"durationDays": 30, "price": 399.00}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions/renew", catalog.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subStatus").value("RENEWED"));

        Long defaultPlanDetailsId = planDetailsRepository
                .findByPlanIdAndStatusAndIsDefaultTrue(planId, PlanDetailsStatus.ACTIVE)
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/v1/users/{userId}/subscriptions/current", catalog.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planDetailsId").value(defaultPlanDetailsId.intValue()));
    }

    @Test
    void subscribeUsesDefaultPlanDetailsEvenWhenOlderIdRequested() throws Exception {
        Long planId = planDetailsRepository.findById(catalog.getPlanDetailsId()).orElseThrow().getPlanId();
        Long oldPlanDetailsId = catalog.getPlanDetailsId();

        mockMvc.perform(post("/api/v1/membership/plans/{planId}/details", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"durationDays": 30, "price": 319.00}
                                """))
                .andExpect(status().isOk());

        Long defaultPlanDetailsId = planDetailsRepository
                .findByPlanIdAndStatusAndIsDefaultTrue(planId, PlanDetailsStatus.ACTIVE)
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions", catalog.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planDetailsId": %d, "tierDetailsId": %d}
                                """.formatted(oldPlanDetailsId, catalog.getSilverTierDetailsId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planDetailsId").value(defaultPlanDetailsId.intValue()));
    }

    @Test
    void getPlansReturnsActivePlans() throws Exception {
        mockMvc.perform(get("/api/v1/membership/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data[0].code").value("MONTHLY"));
    }

    @Test
    void getTiersReturnsActiveTiers() throws Exception {
        mockMvc.perform(get("/api/v1/membership/tiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data[0].code").value("SILVER"))
                .andExpect(jsonPath("$.data[1].code").value("GOLD"));
    }

    // @Test
    // void getPlansIsCached() throws Exception {
    //     mockMvc.perform(get("/api/v1/membership/plans")).andExpect(status().isOk());
    //     mockMvc.perform(get("/api/v1/membership/plans")).andExpect(status().isOk());
    //
    //     Cache cache = cacheManager.getCache(Constants.CACHE_MEMBERSHIP_PLANS);
    //     assertThat(cache).isNotNull();
    //     assertThat(((ConcurrentMapCache) cache).getNativeCache()).isNotEmpty();
    // }

    @Test
    void getUserReturnsUserDetails() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}", catalog.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    void createUserWithCohortCodeReturnsItInResponse() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "VIP User",
                                  "email": "vip@example.com",
                                  "phone": "8888888888",
                                  "cohortCode": "VIP"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cohortCode").value("VIP"));
    }

    @Test
    void subscribeWithSilverTierSucceeds() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions", catalog.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planDetailsId": %d, "tierDetailsId": %d}
                                """.formatted(catalog.getPlanDetailsId(), catalog.getSilverTierDetailsId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.data.tierCode").value("SILVER"));
    }

    @Test
    void subscribeWithGoldTierWithoutOrdersFails() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions", catalog.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planDetailsId": %d, "tierDetailsId": %d}
                                """.formatted(catalog.getPlanDetailsId(), catalog.getGoldTierDetailsId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.message").value("User does not qualify for tier: GOLD"));
    }

    @Test
    void getCurrentSubscriptionWhenActiveSucceeds() throws Exception {
        saveActiveSubscription();

        mockMvc.perform(get("/api/v1/users/{userId}/subscriptions/current", catalog.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.tierCode").value("SILVER"));
    }

    @Test
    void getCurrentSubscriptionWhenExpiredReturnsNotFound() throws Exception {
        subscriptionRepository.save(Subscription.builder()
                .userId(catalog.getUserId())
                .planDetailsId(catalog.getPlanDetailsId())
                .tierDetailsId(catalog.getSilverTierDetailsId())
                .startsAt(LocalDateTime.now().minusDays(40))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .status(SubscriptionStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/users/{userId}/subscriptions/current", catalog.getUserId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSubscriptionHistoryReturnsEntries() throws Exception {
        saveActiveSubscription();

        mockMvc.perform(get("/api/v1/users/{userId}/subscriptions", catalog.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].tierCode").value("SILVER"));
    }

    @Test
    void cancelSubscriptionSucceeds() throws Exception {
        saveActiveSubscription();

        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions/cancel", catalog.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void renewSubscriptionExtendsExpiry() throws Exception {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(5);
        subscriptionRepository.save(Subscription.builder()
                .userId(catalog.getUserId())
                .planDetailsId(catalog.getPlanDetailsId())
                .tierDetailsId(catalog.getSilverTierDetailsId())
                .startsAt(LocalDateTime.now().minusDays(25))
                .expiresAt(expiresAt)
                .status(SubscriptionStatus.ACTIVE)
                .build());

        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions/renew", catalog.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subStatus").value("RENEWED"));
    }

    @Test
    void changePlanSucceeds() throws Exception {
        saveActiveSubscription();

        mockMvc.perform(put("/api/v1/users/{userId}/subscriptions/plan", catalog.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPlanDetailsId": %d}
                                """.formatted(catalog.getAlternatePlanDetailsId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planCode").value("YEARLY"));
    }

    @Test
    void changeTierToGoldWithoutQualificationFails() throws Exception {
        saveActiveSubscription();

        mockMvc.perform(put("/api/v1/users/{userId}/subscriptions/tier", catalog.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newTierDetailsId": %d}
                                """.formatted(catalog.getGoldTierDetailsId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User does not qualify for tier: GOLD"));
    }

    @Test
    void checkoutBenefitsApplyMembershipDiscount() throws Exception {
        saveActiveSubscription();

        mockMvc.perform(post("/api/v1/users/{userId}/checkout/benefits", catalog.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"itemId":"SKU-1","categoryId":"GROCERIES","lineTotal":1000}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.membershipActive").value(true))
                .andExpect(jsonPath("$.data.cartSubtotal").value(1000));
    }

    @Test
    void checkoutBenefitsWithoutMembershipReturnsInactive() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/checkout/benefits", catalog.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"itemId":"SKU-1","categoryId":"GROCERIES","lineTotal":1000}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.membershipActive").value(false));
    }

    private void saveActiveSubscription() {
        subscriptionRepository.save(Subscription.builder()
                .userId(catalog.getUserId())
                .planDetailsId(catalog.getPlanDetailsId())
                .tierDetailsId(catalog.getSilverTierDetailsId())
                .startsAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(29))
                .status(SubscriptionStatus.ACTIVE)
                .build());
    }
}

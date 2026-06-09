package com.example.butex;

import com.example.butex.entity.Subscription;
import com.example.butex.enums.SubscriptionStatus;
import com.example.butex.repository.SubscriptionRepository;
import com.example.butex.support.CatalogTestData;
import com.example.butex.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MembershipApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CatalogTestData catalogTestData;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private CacheManager cacheManager;

    private CatalogTestData.SeededCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = catalogTestData.seed();
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

    @Test
    void getPlansIsCached() throws Exception {
        mockMvc.perform(get("/api/v1/membership/plans")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/membership/plans")).andExpect(status().isOk());

        Cache cache = cacheManager.getCache(Constants.CACHE_MEMBERSHIP_PLANS);
        assertThat(cache).isNotNull();
        assertThat(((ConcurrentMapCache) cache).getNativeCache()).isNotEmpty();
    }

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
                                {"planDetailsId": %d}
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
                                {"tierDetailsId": %d}
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

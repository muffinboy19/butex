package com.example.butex;

import com.example.butex.entity.Subscription;
import com.example.butex.entity.UserOrder;
import com.example.butex.enums.SubscriptionStatus;
import com.example.butex.enums.UserSubscriptionHistoryAction;
import com.example.butex.repository.SubscriptionRepository;
import com.example.butex.repository.UserOrderRepository;
import com.example.butex.repository.UserSubscriptionHistoryRepository;
import com.example.butex.service.SubscriptionExpiryJobExecutor;
import com.example.butex.support.CatalogTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
class UserSubscriptionHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CatalogTestData catalogTestData;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserSubscriptionHistoryRepository historyRepository;

    @Autowired
    private UserOrderRepository userOrderRepository;

    @Autowired
    private SubscriptionExpiryJobExecutor subscriptionExpiryJobExecutor;

    private CatalogTestData.SeededCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = catalogTestData.seed();
    }

    @Test
    void subscribeWritesCreatedHistory() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions", catalog.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planDetailsId": %d, "tierDetailsId": %d}
                                """.formatted(catalog.getPlanDetailsId(), catalog.getSilverTierDetailsId())))
                .andExpect(status().isOk());

        assertThat(historyRepository.findByUserIdOrderByActionAtDesc(catalog.getUserId()))
                .extracting("action")
                .containsExactly(UserSubscriptionHistoryAction.CREATED);
    }

    @Test
    void renewWritesRenewedHistory() throws Exception {
        saveActiveSubscription();

        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions/renew", catalog.getUserId()))
                .andExpect(status().isOk());

        assertThat(historyRepository.findByUserIdOrderByActionAtDesc(catalog.getUserId()))
                .extracting("action")
                .containsExactly(UserSubscriptionHistoryAction.RENEWED);
    }

    @Test
    void changePlanWritesPlanChangedHistory() throws Exception {
        saveActiveSubscription();

        mockMvc.perform(put("/api/v1/users/{userId}/subscriptions/plan", catalog.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPlanDetailsId": %d}
                                """.formatted(catalog.getAlternatePlanDetailsId())))
                .andExpect(status().isOk());

        assertThat(historyRepository.findByUserIdOrderByActionAtDesc(catalog.getUserId()))
                .extracting("action")
                .containsExactly(UserSubscriptionHistoryAction.PLAN_CHANGED);
    }

    @Test
    void changeTierWritesTierUpgradedHistory() throws Exception {
        saveActiveSubscription();
        seedGoldQualifyingOrders();

        mockMvc.perform(put("/api/v1/users/{userId}/subscriptions/tier", catalog.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newTierDetailsId": %d}
                                """.formatted(catalog.getGoldTierDetailsId())))
                .andExpect(status().isOk());

        assertThat(historyRepository.findByUserIdOrderByActionAtDesc(catalog.getUserId()))
                .extracting("action")
                .containsExactly(UserSubscriptionHistoryAction.TIER_UPGRADED);
    }

    @Test
    void cancelWritesCancelledHistory() throws Exception {
        saveActiveSubscription();

        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions/cancel", catalog.getUserId()))
                .andExpect(status().isOk());

        assertThat(historyRepository.findByUserIdOrderByActionAtDesc(catalog.getUserId()))
                .extracting("action")
                .containsExactly(UserSubscriptionHistoryAction.CANCELLED);
    }

    @Test
    void expireWritesExpiredHistory() {
        Subscription subscription = saveActiveSubscription();

        subscriptionExpiryJobExecutor.expire(subscription);

        assertThat(historyRepository.findByUserIdOrderByActionAtDesc(catalog.getUserId()))
                .extracting("action")
                .containsExactly(UserSubscriptionHistoryAction.EXPIRED);
    }

    @Test
    void eventsEndpointReturnsActionHistoryAfterLifecycle() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions", catalog.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planDetailsId": %d, "tierDetailsId": %d}
                                """.formatted(catalog.getPlanDetailsId(), catalog.getSilverTierDetailsId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions/renew", catalog.getUserId()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/users/{userId}/subscriptions/plan", catalog.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPlanDetailsId": %d}
                                """.formatted(catalog.getAlternatePlanDetailsId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/{userId}/subscriptions/events", catalog.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].action").value("PLAN_CHANGED"))
                .andExpect(jsonPath("$.data[1].action").value("RENEWED"))
                .andExpect(jsonPath("$.data[2].action").value("CREATED"));
    }

    @Test
    void subscriptionListEndpointDoesNotIncludeActionHistory() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions", catalog.getUserId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planDetailsId": %d, "tierDetailsId": %d}
                                """.formatted(catalog.getPlanDetailsId(), catalog.getSilverTierDetailsId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users/{userId}/subscriptions/renew", catalog.getUserId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/{userId}/subscriptions", catalog.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].planCode").exists())
                .andExpect(jsonPath("$.data[0].action").doesNotExist());
    }

    private Subscription saveActiveSubscription() {
        return subscriptionRepository.save(Subscription.builder()
                .userId(catalog.getUserId())
                .planDetailsId(catalog.getPlanDetailsId())
                .tierDetailsId(catalog.getSilverTierDetailsId())
                .startsAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(29))
                .status(SubscriptionStatus.ACTIVE)
                .build());
    }

    private void seedGoldQualifyingOrders() {
        LocalDateTime orderedAt = LocalDateTime.now();
        for (int i = 0; i < 10; i++) {
            userOrderRepository.save(UserOrder.builder()
                    .userId(catalog.getUserId())
                    .orderValue(new BigDecimal("600.00"))
                    .orderedAt(orderedAt)
                    .build());
        }
    }
}

package com.example.butex.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseIndexInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS uk_subscriptions_user_active
                    ON subscriptions (user_id)
                    WHERE status = 'ACTIVE'
                    """);
            log.info("Ensured partial unique index on active subscriptions per user");
        } catch (Exception ex) {
            log.warn("Could not create partial unique index uk_subscriptions_user_active: {}", ex.getMessage());
        }
    }
}

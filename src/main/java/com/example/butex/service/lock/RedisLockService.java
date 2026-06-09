package com.example.butex.service.lock;

import com.example.butex.exception.LockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "butex.lock.provider", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class RedisLockService implements LockService {

    private static final String LOCK_VALUE = "true";
    private static final int LOCK_TTL_SECONDS = 120;

    private final StringRedisTemplate redisTemplate;

    @Override
    public void getLockOnKey(String key) {
        try {
            Boolean added = redisTemplate.opsForValue()
                    .setIfAbsent(key, LOCK_VALUE, Duration.ofSeconds(LOCK_TTL_SECONDS));
            if (!Boolean.TRUE.equals(added)) {
                throw new LockException("Request is already in process.");
            }
        } catch (LockException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Exception in getLockOnKey {}", ex.getMessage());
            throw new LockException(String.format("Lock Exception: %s", ex.getMessage()));
        }
    }

    @Override
    public void releaseLockOnKey(String key) {
        redisTemplate.delete(key);
    }
}

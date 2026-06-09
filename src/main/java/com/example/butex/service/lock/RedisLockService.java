package com.example.butex.service.lock;

import com.example.butex.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "butex.lock.provider", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class RedisLockService implements DistributedLockService {

    private static final String LOCK_VALUE_PREFIX = "butex-lock:";
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final Map<String, String> heldLocks = new ConcurrentHashMap<>();

    @Value("${butex.lock.acquire-timeout-ms:10000}")
    private long acquireTimeoutMs;

    @Value("${butex.lock.ttl-seconds:30}")
    private long lockTtlSeconds;

    @Override
    public void getLockOnKey(String key) {
        String lockToken = LOCK_VALUE_PREFIX + UUID.randomUUID();
        long deadline = System.currentTimeMillis() + acquireTimeoutMs;

        while (System.currentTimeMillis() < deadline) {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(key, lockToken, Duration.ofSeconds(lockTtlSeconds));
            if (Boolean.TRUE.equals(acquired)) {
                heldLocks.put(key, lockToken);
                log.debug("Acquired Redis lock on key={}", key);
                return;
            }
            sleepBriefly();
        }

        throw new BusinessException("Could not acquire lock for key: " + key);
    }

    @Override
    public void releaseLockOnKey(String key) {
        String lockToken = heldLocks.remove(key);
        if (lockToken == null) {
            log.warn("No held lock token found for key={}", key);
            return;
        }

        Long released = redisTemplate.execute(
                RELEASE_SCRIPT,
                Collections.singletonList(key),
                lockToken
        );
        if (released == null || released == 0L) {
            log.warn("Redis lock for key={} was not released (expired or owned by another instance)", key);
            return;
        }
        log.debug("Released Redis lock on key={}", key);
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(50L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Interrupted while waiting for lock");
        }
    }
}

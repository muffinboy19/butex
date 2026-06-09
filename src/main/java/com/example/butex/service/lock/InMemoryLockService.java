package com.example.butex.service.lock;

import com.example.butex.exception.LockException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "butex.lock.provider", havingValue = "in-memory")
public class InMemoryLockService implements LockService {

    private final ConcurrentHashMap<String, Boolean> locks = new ConcurrentHashMap<>();

    @Override
    public void getLockOnKey(String key) {
        if (locks.putIfAbsent(key, Boolean.TRUE) != null) {
            throw new LockException("Request is already in process.");
        }
    }

    @Override
    public void releaseLockOnKey(String key) {
        locks.remove(key);
    }
}

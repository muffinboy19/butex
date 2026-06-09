package com.example.butex.service.lock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@ConditionalOnProperty(name = "butex.lock.provider", havingValue = "in-memory")
public class InMemoryLockService implements DistributedLockService {

    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public void getLockOnKey(String key) {
        ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());
        lock.lock();
    }

    @Override
    public void releaseLockOnKey(String key) {
        ReentrantLock lock = locks.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}

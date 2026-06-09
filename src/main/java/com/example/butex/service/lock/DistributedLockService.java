package com.example.butex.service.lock;

public interface DistributedLockService {

    void getLockOnKey(String key);

    void releaseLockOnKey(String key);
}

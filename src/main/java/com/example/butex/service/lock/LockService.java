package com.example.butex.service.lock;

public interface LockService {

    void getLockOnKey(String key);

    void releaseLockOnKey(String key);

    default void runWithLock(String lockName, Runnable runnable) {
        getLockOnKey(lockName);
        try {
            runnable.run();
        } finally {
            releaseLockOnKey(lockName);
        }
    }
}

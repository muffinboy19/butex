package com.example.butex.service.lock;

public interface LockService {

    void getLockOnKey(String key);

    void releaseLockOnKey(String key);

}

package org.example.owoonwan.session.repository;

import java.time.Instant;

public interface LoginLockRepository {

    void acquire(String loginId, Instant now, Instant expiresAt);

    void release(String loginId);
}

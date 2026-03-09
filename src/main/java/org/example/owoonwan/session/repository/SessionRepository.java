package org.example.owoonwan.session.repository;

import org.example.owoonwan.session.domain.Session;

import java.time.Instant;
import java.util.Optional;

public interface SessionRepository {

    String create(String userId, String loginId, Instant now, Instant expiresAt);

    Optional<Session> findByToken(String token);

    void deactivateByToken(String token, Instant now);

    void deactivateAllByUserId(String userId, Instant now);

    void touchLastSeen(String token, Instant now);
}

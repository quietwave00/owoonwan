package org.example.owoonwan.session.domain;

import java.time.Instant;

public record Session(
        String token,
        String userId,
        String loginId,
        boolean active,
        Instant createdAt,
        Instant lastSeenAt,
        Instant expiresAt
) {
}

package org.example.owoonwan.user.domain;

import java.time.Instant;

public record User(
        String id,
        String loginId,
        String nicknameId,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant deletedAt,
        Instant lastLoginAt,
        boolean kakkdugi,
        String pledgeId
) {
}

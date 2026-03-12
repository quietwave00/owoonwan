package org.example.owoonwan.user.domain;

import java.time.Instant;

public record User(
        String id,
        String loginId,
        String nicknameId,
        String nicknameDisplay,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant deletedAt,
        Instant lastLoginAt,
        boolean kakkdugi,
        String pledgeId
) {
    public User(
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
        this(id, loginId, nicknameId, null, role, status, createdAt, deletedAt, lastLoginAt, kakkdugi, pledgeId);
    }

    public String displayNickname() {
        if (nicknameDisplay != null && !nicknameDisplay.isBlank()) {
            return nicknameDisplay;
        }
        if (nicknameId != null && !nicknameId.isBlank()) {
            return nicknameId;
        }
        return "";
    }
}

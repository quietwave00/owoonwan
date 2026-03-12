package org.example.owoonwan.auth.dto;

import org.example.owoonwan.user.domain.UserRole;

import java.time.Instant;

public record AuthenticatedUser(
        String userId,
        String loginId,
        String nicknameId,
        String nicknameDisplay,
        UserRole role,
        String token,
        Instant expiresAt
) {
    public AuthenticatedUser(
            String userId,
            String loginId,
            String nicknameId,
            UserRole role,
            String token
    ) {
        this(userId, loginId, nicknameId, null, role, token, null);
    }

    public AuthenticatedUser(
            String userId,
            String loginId,
            String nicknameId,
            String nicknameDisplay,
            UserRole role,
            String token
    ) {
        this(userId, loginId, nicknameId, nicknameDisplay, role, token, null);
    }
}

package org.example.owoonwan.auth.dto;

import org.example.owoonwan.user.domain.UserRole;

import java.time.Instant;

public record AuthMeResponse(
        String uid,
        String loginId,
        String nicknameId,
        UserRole role,
        Instant expiresAt
) {
}

package org.example.owoonwan.auth.dto;

import java.time.Instant;

public record AuthLoginResponse(
        String sessionToken,
        Instant expiresAt,
        String uid,
        String loginId,
        String nicknameId,
        String nicknameDisplay
) {
}

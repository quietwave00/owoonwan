package org.example.owoonwan.auth.dto;

import org.example.owoonwan.user.domain.UserRole;

public record AuthenticatedUser(
        String userId,
        String loginId,
        String nicknameId,
        String nicknameDisplay,
        UserRole role,
        String token
) {
    public AuthenticatedUser(
            String userId,
            String loginId,
            String nicknameId,
            UserRole role,
            String token
    ) {
        this(userId, loginId, nicknameId, null, role, token);
    }
}

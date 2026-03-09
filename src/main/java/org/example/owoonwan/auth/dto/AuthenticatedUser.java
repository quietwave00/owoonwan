package org.example.owoonwan.auth.dto;

import org.example.owoonwan.user.domain.UserRole;

public record AuthenticatedUser(
        String userId,
        String loginId,
        String nicknameId,
        UserRole role,
        String token
) {
}

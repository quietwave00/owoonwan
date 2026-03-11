package org.example.owoonwan.user.dto;

import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserRole;
import org.example.owoonwan.user.domain.UserStatus;

import java.time.Instant;

public record UserResponse(
        String uid,
        String loginId,
        String nicknameId,
        boolean kakkdugi,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant deletedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.id(),
                user.loginId(),
                user.nicknameId(),
                user.kakkdugi(),
                user.role(),
                user.status(),
                user.createdAt(),
                user.deletedAt()
        );
    }
}

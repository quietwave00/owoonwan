package org.example.owoonwan.nickname.dto;

import org.example.owoonwan.nickname.domain.Nickname;

public record NicknameResponse(
        String nicknameId,
        String display,
        boolean isActive,
        String assignedTo
) {
    public static NicknameResponse from(Nickname nickname) {
        return new NicknameResponse(
                nickname.id(),
                nickname.display(),
                nickname.active(),
                nickname.assignedTo()
        );
    }
}

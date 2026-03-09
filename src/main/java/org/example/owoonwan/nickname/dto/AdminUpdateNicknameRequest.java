package org.example.owoonwan.nickname.dto;

public record AdminUpdateNicknameRequest(
        String display,
        Boolean isActive
) {
}

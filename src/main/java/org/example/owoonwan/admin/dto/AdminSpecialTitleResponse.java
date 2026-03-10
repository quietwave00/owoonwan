package org.example.owoonwan.admin.dto;

import org.example.owoonwan.user.domain.User;

public record AdminSpecialTitleResponse(
        String uid,
        boolean kakkdugi
) {

    public static AdminSpecialTitleResponse from(User user) {
        return new AdminSpecialTitleResponse(user.id(), user.kakkdugi());
    }
}

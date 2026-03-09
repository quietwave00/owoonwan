package org.example.owoonwan.user.dto;

import org.example.owoonwan.user.domain.UserRole;

public record AdminUpdateUserRoleRequest(
        UserRole role
) {
}

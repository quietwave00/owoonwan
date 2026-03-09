package org.example.owoonwan.stats.dto;

import org.example.owoonwan.user.domain.UserRole;

public record MonthlyBoardMemberResponse(
        String uid,
        String nickname,
        UserRole role,
        int monthlyCount
) {
}

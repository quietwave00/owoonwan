package org.example.owoonwan.checkin.dto;

import org.example.owoonwan.user.domain.UserRole;

import java.util.List;

public record WeeklyBoardMemberResponse(
        String uid,
        String nickname,
        UserRole role,
        int weeklyCount,
        List<CheckinDayResponse> days
) {
}

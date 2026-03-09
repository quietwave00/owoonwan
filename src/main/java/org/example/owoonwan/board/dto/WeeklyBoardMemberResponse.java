package org.example.owoonwan.board.dto;

import org.example.owoonwan.user.domain.UserRole;

import java.util.List;

public record WeeklyBoardMemberResponse(
        String uid,
        String nickname,
        UserRole role,
        List<WeeklyBoardDayResponse> days,
        int weeklyCount
) {
}

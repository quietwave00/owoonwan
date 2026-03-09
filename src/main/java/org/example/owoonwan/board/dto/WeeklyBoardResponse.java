package org.example.owoonwan.board.dto;

import java.util.List;

public record WeeklyBoardResponse(
        String weekKey,
        String weekStartDate,
        String weekEndDate,
        List<WeeklyBoardMemberResponse> members
) {
}

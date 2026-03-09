package org.example.owoonwan.stats.dto;

import java.util.List;

public record MonthlyBoardResponse(
        String monthKey,
        List<MonthlyBoardMemberResponse> members
) {
}

package org.example.owoonwan.admin.dto;

import java.util.List;

public record AdminWeeklyStatsResponse(
        String weekKey,
        String weekStartDate,
        String weekEndDate,
        List<AdminStatsMemberResponse> members
) {
}

package org.example.owoonwan.stats.dto;

public record UserSummaryResponse(
        String uid,
        String nickname,
        String currentWeekKey,
        int currentWeekCount,
        String previousWeekKey,
        int previousWeekCount,
        String currentMonthKey,
        int currentMonthCount,
        String previousMonthKey,
        int previousMonthCount
) {
}

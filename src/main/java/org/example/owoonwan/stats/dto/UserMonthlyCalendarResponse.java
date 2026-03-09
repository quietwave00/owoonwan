package org.example.owoonwan.stats.dto;

import org.example.owoonwan.checkin.dto.CheckinDayResponse;

import java.util.List;

public record UserMonthlyCalendarResponse(
        String uid,
        String nickname,
        String monthKey,
        List<CheckinDayResponse> days,
        int monthlyCount,
        List<Integer> weeklyCounts
) {
}

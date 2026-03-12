package org.example.owoonwan.admin.dto;

import java.util.List;

public record AdminMonthlyStatsResponse(
        String monthKey,
        List<AdminStatsMemberResponse> members
) {
}

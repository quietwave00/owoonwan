package org.example.owoonwan.checkin.dto;

import java.util.List;

public record CheckinPeriodResponse(
        String uid,
        String periodKey,
        String startDate,
        String endDate,
        int presentCount,
        List<CheckinDayResponse> days
) {
}

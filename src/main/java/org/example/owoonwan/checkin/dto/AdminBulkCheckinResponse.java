package org.example.owoonwan.checkin.dto;

import java.util.List;

public record AdminBulkCheckinResponse(
        String date,
        int processedCount,
        List<CheckinResponse> checkins
) {
}

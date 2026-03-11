package org.example.owoonwan.checkin.dto;

import java.util.List;

public record UserBulkCheckinRequest(
        String date,
        List<String> dates
) {
}

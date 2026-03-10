package org.example.owoonwan.checkin.dto;

import java.util.List;

public record AdminBulkCheckinRequest(
        String date,
        List<String> userIds
) {
}

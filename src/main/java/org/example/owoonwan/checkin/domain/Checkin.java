package org.example.owoonwan.checkin.domain;

import java.time.Instant;

public record Checkin(
        String id,
        String userId,
        String date,
        String weekKey,
        String monthKey,
        CheckinStatus status,
        Instant checkedAt
) {
}

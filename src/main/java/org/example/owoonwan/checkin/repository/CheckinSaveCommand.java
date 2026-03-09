package org.example.owoonwan.checkin.repository;

import org.example.owoonwan.checkin.domain.CheckinStatus;

import java.time.Instant;

public record CheckinSaveCommand(
        String documentId,
        String userId,
        String date,
        String weekKey,
        String monthKey,
        CheckinStatus status,
        Instant checkedAt
) {
}

package org.example.owoonwan.checkin.dto;

import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;

import java.time.Instant;

public record CheckinResponse(
        String uid,
        String date,
        String weekKey,
        String monthKey,
        CheckinStatus status,
        Instant checkedAt
) {
    public static CheckinResponse from(Checkin checkin) {
        return new CheckinResponse(
                checkin.userId(),
                checkin.date(),
                checkin.weekKey(),
                checkin.monthKey(),
                checkin.status(),
                checkin.checkedAt()
        );
    }
}

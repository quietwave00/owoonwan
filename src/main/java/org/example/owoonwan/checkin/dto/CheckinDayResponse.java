package org.example.owoonwan.checkin.dto;

import org.example.owoonwan.checkin.domain.CheckinStatus;

public record CheckinDayResponse(
        String date,
        CheckinStatus status,
        boolean canToggle
) {
}

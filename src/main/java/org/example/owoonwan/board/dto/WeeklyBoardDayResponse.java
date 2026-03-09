package org.example.owoonwan.board.dto;

import org.example.owoonwan.checkin.domain.CheckinStatus;

public record WeeklyBoardDayResponse(
        String date,
        CheckinStatus status,
        boolean canCheckinAction
) {
}

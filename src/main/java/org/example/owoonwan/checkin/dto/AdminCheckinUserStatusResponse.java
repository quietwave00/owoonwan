package org.example.owoonwan.checkin.dto;

import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.user.domain.UserRole;

import java.time.Instant;

public record AdminCheckinUserStatusResponse(
        String uid,
        String nickname,
        UserRole role,
        CheckinStatus status,
        boolean checked,
        Instant checkedAt
) {
}

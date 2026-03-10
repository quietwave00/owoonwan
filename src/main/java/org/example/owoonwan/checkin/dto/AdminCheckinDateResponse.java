package org.example.owoonwan.checkin.dto;

import java.util.List;

public record AdminCheckinDateResponse(
        String date,
        int checkedCount,
        List<AdminCheckinUserStatusResponse> users
) {
}

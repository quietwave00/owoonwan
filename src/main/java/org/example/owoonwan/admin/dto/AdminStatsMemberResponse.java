package org.example.owoonwan.admin.dto;

import java.util.List;

public record AdminStatsMemberResponse(
        String uid,
        String nickname,
        int count,
        List<String> badges
) {
}

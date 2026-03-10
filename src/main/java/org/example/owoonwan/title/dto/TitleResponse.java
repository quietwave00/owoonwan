package org.example.owoonwan.title.dto;

import java.util.List;

public record TitleResponse(
        String uid,
        String weekKey,
        String monthKey,
        TitleScopeResponse weekly,
        TitleScopeResponse monthly,
        SpecialTitleResponse special,
        List<String> effectiveBadges
) {
}

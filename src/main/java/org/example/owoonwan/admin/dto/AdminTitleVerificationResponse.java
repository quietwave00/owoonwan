package org.example.owoonwan.admin.dto;

import org.example.owoonwan.title.dto.TitleResponse;

import java.util.List;

public record AdminTitleVerificationResponse(
        String weekKey,
        String monthKey,
        List<TitleResponse> titles
) {
}

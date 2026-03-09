package org.example.owoonwan.nickname.domain;

import java.time.Instant;

public record Nickname(
        String id,
        String display,
        boolean active,
        String assignedTo,
        Instant createdAt,
        Instant updatedAt
) {
}

package org.example.owoonwan.pledge.domain;

import java.time.Instant;

public record Pledge(
        String userId,
        String text,
        Instant updatedAt,
        int version
) {
}

package org.example.owoonwan.pledge.dto;

import org.example.owoonwan.pledge.domain.Pledge;

import java.time.Instant;

public record PledgeResponse(
        String uid,
        String nickname,
        String text,
        Instant updatedAt,
        int version,
        boolean mine
) {

    public static PledgeResponse from(Pledge pledge, String nickname, boolean mine) {
        return new PledgeResponse(
                pledge.userId(),
                nickname,
                pledge.text(),
                pledge.updatedAt(),
                pledge.version(),
                mine
        );
    }
}

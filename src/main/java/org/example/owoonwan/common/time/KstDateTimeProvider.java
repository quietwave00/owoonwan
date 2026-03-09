package org.example.owoonwan.common.time;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
public class KstDateTimeProvider {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final Clock utcClock;

    public Instant nowUtc() {
        return Instant.now(utcClock);
    }

    public ZonedDateTime nowKst() {
        return nowUtc().atZone(KST);
    }

    public LocalDate todayKst() {
        return nowKst().toLocalDate();
    }
}

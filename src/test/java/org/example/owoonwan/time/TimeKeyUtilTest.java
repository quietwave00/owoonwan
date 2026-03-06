package org.example.owoonwan.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeKeyUtilTest {

    @Test
    void shouldDeriveKstDateWeekMonthAtWeekBoundary() {
        Instant instant = Instant.parse("2026-03-01T15:00:00Z");

        assertEquals("2026-03-02", TimeKeyUtil.deriveDateString(instant));
        assertEquals("2026-W10", TimeKeyUtil.deriveWeekKey(instant));
        assertEquals("2026-03", TimeKeyUtil.deriveMonthKey(instant));
    }

    @Test
    void shouldHandleLeapDayInKst() {
        Instant instant = Instant.parse("2024-02-29T01:30:00Z");

        assertEquals("2024-02-29", TimeKeyUtil.deriveDateString(instant));
        assertEquals("2024-W09", TimeKeyUtil.deriveWeekKey(instant));
        assertEquals("2024-02", TimeKeyUtil.deriveMonthKey(instant));
    }
}

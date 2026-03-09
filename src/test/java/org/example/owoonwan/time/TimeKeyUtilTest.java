package org.example.owoonwan.time;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeKeyUtilTest {

    @Test
    @DisplayName("KST 주간 경계에서 date/week/month 키를 올바르게 계산한다")
    void shouldDeriveKstDateWeekMonthAtWeekBoundary() {
        Instant instant = Instant.parse("2026-03-01T15:00:00Z");

        assertEquals("2026-03-02", TimeKeyUtil.deriveDateString(instant));
        assertEquals("2026-W10", TimeKeyUtil.deriveWeekKey(instant));
        assertEquals("2026-03", TimeKeyUtil.deriveMonthKey(instant));
    }

    @Test
    @DisplayName("윤년 2월 29일의 KST 키를 올바르게 계산한다")
    void shouldHandleLeapDayInKst() {
        Instant instant = Instant.parse("2024-02-29T01:30:00Z");

        assertEquals("2024-02-29", TimeKeyUtil.deriveDateString(instant));
        assertEquals("2024-W09", TimeKeyUtil.deriveWeekKey(instant));
        assertEquals("2024-02", TimeKeyUtil.deriveMonthKey(instant));
    }
}

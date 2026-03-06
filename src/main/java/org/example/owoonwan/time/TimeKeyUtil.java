package org.example.owoonwan.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;

public final class TimeKeyUtil {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private TimeKeyUtil() {
    }

    public static String deriveDateString(Instant instant) {
        return toKst(instant).format(DATE);
    }

    public static LocalDate deriveKstDate(Instant instant) {
        return toKst(instant).toLocalDate();
    }

    public static String deriveMonthKey(Instant instant) {
        return toKst(instant).format(MONTH);
    }

    public static String deriveWeekKey(Instant instant) {
        ZonedDateTime kst = toKst(instant);
        int week = kst.get(WeekFields.ISO.weekOfWeekBasedYear());
        int year = kst.get(WeekFields.ISO.weekBasedYear());
        return String.format("%04d-W%02d", year, week);
    }

    private static ZonedDateTime toKst(Instant instant) {
        return instant.atZone(KST);
    }
}

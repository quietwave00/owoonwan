package org.example.owoonwan.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.admin.dto.AdminMonthlyStatsResponse;
import org.example.owoonwan.admin.dto.AdminStatsMemberResponse;
import org.example.owoonwan.admin.dto.AdminWeeklyStatsResponse;
import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.repository.CheckinRepository;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.time.TimeKeyUtil;
import org.example.owoonwan.title.service.TitleBadgeAssembler;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserStatus;
import org.example.owoonwan.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CheckinRepository checkinRepository;
    private final UserRepository userRepository;
    private final TitleBadgeAssembler titleBadgeAssembler;
    private final KstDateTimeProvider dateTimeProvider;

    public AdminWeeklyStatsResponse getWeeklyStats(String date) {
        LocalDate targetDate = parseDateOrToday(date);
        LocalDate weekStart = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        String weekKey = TimeKeyUtil.deriveWeekKey(weekStart.atStartOfDay(KST_ZONE).toInstant());

        List<Checkin> weeklyCheckins = checkinRepository.findByWeekKey(weekKey);
        Map<String, Integer> presentCounts = countPresentByUserId(weeklyCheckins);
        List<AdminStatsMemberResponse> members = buildMembers(
                presentCounts,
                (user, count) -> titleBadgeAssembler.buildWeeklyBadges(count, user.kakkdugi())
        );

        return new AdminWeeklyStatsResponse(
                weekKey,
                weekStart.format(DATE_FORMATTER),
                weekEnd.format(DATE_FORMATTER),
                members
        );
    }

    public AdminMonthlyStatsResponse getMonthlyStats(String month) {
        YearMonth targetMonth = parseMonthOrCurrent(month);
        String monthKey = targetMonth.format(MONTH_FORMATTER);
        List<Checkin> monthlyCheckins = checkinRepository.findByMonthKey(monthKey);
        Map<String, Integer> presentCounts = countPresentByUserId(monthlyCheckins);
        List<AdminStatsMemberResponse> members = buildMembers(
                presentCounts,
                (user, count) -> titleBadgeAssembler.buildMonthlyBadges(count, user.kakkdugi())
        );

        return new AdminMonthlyStatsResponse(monthKey, members);
    }

    private Map<String, Integer> countPresentByUserId(List<Checkin> checkins) {
        Map<String, Integer> presentCounts = new LinkedHashMap<>();
        for (Checkin checkin : checkins) {
            if (checkin.status() != CheckinStatus.PRESENT) {
                continue;
            }
            presentCounts.merge(checkin.userId(), 1, Integer::sum);
        }
        return presentCounts;
    }

    private List<AdminStatsMemberResponse> buildMembers(
            Map<String, Integer> presentCounts,
            java.util.function.BiFunction<User, Integer, List<String>> badgesBuilder
    ) {
        if (presentCounts.isEmpty()) {
            return List.of();
        }
        return userRepository.findByIds(List.copyOf(presentCounts.keySet())).stream()
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .map(user -> {
                    int count = presentCounts.getOrDefault(user.id(), 0);
                    return new AdminStatsMemberResponse(
                            user.id(),
                            user.displayNickname(),
                            count,
                            badgesBuilder.apply(user, count)
                    );
                })
                .sorted(Comparator.comparingInt(AdminStatsMemberResponse::count).reversed()
                        .thenComparing(AdminStatsMemberResponse::nickname))
                .toList();
    }

    private LocalDate parseDateOrToday(String date) {
        if (date == null || date.isBlank()) {
            return dateTimeProvider.todayKst();
        }
        try {
            return LocalDate.parse(date, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.CHECKIN_INVALID_DATE);
        }
    }

    private YearMonth parseMonthOrCurrent(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.from(dateTimeProvider.todayKst());
        }
        try {
            return YearMonth.parse(month, MONTH_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.CHECKIN_INVALID_MONTH);
        }
    }
}

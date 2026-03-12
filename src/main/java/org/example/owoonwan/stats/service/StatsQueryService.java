package org.example.owoonwan.stats.service;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.dto.CheckinDayResponse;
import org.example.owoonwan.checkin.repository.CheckinRepository;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.stats.dto.MonthlyBoardMemberResponse;
import org.example.owoonwan.stats.dto.MonthlyBoardResponse;
import org.example.owoonwan.stats.dto.UserMonthlyCalendarResponse;
import org.example.owoonwan.stats.dto.UserSummaryResponse;
import org.example.owoonwan.time.TimeKeyUtil;
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
public class StatsQueryService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CheckinRepository checkinRepository;
    private final UserRepository userRepository;
    private final KstDateTimeProvider dateTimeProvider;

    public MonthlyBoardResponse getMonthlyBoard(String month) {
        YearMonth targetMonth = parseMonthOrCurrent(month);
        String monthKey = targetMonth.format(MONTH_FORMATTER);

        List<MonthlyBoardMemberResponse> members = userRepository.findAll().stream()
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .map(user -> toMonthlyBoardMember(user, monthKey))
                .sorted(Comparator.comparingInt(MonthlyBoardMemberResponse::monthlyCount).reversed()
                        .thenComparing(MonthlyBoardMemberResponse::nickname))
                .toList();

        return new MonthlyBoardResponse(monthKey, members);
    }

    public UserMonthlyCalendarResponse getUserCalendar(String userId, String month, String viewerUserId) {
        User targetUser = getActiveUser(userId);
        YearMonth targetMonth = parseMonthOrCurrent(month);
        String monthKey = targetMonth.format(MONTH_FORMATTER);
        List<Checkin> checkins = checkinRepository.findByUserIdAndMonthKey(userId, monthKey);

        return new UserMonthlyCalendarResponse(
                targetUser.id(),
                targetUser.displayNickname(),
                monthKey,
                buildCalendarDays(checkins, targetMonth, userId.equals(viewerUserId)),
                countPresent(checkins),
                buildWeeklyCounts(checkins, targetMonth)
        );
    }

    public UserSummaryResponse getUserSummary(String userId) {
        User targetUser = getActiveUser(userId);
        LocalDate today = dateTimeProvider.todayKst();

        LocalDate currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate previousWeekStart = currentWeekStart.minusWeeks(1);
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previousMonth = currentMonth.minusMonths(1);

        List<Checkin> currentWeekCheckins = checkinRepository.findByUserIdAndDateRange(
                userId,
                currentWeekStart.format(DATE_FORMATTER),
                currentWeekStart.plusDays(6).format(DATE_FORMATTER)
        );
        List<Checkin> previousWeekCheckins = checkinRepository.findByUserIdAndDateRange(
                userId,
                previousWeekStart.format(DATE_FORMATTER),
                previousWeekStart.plusDays(6).format(DATE_FORMATTER)
        );
        List<Checkin> currentMonthCheckins = checkinRepository.findByUserIdAndMonthKey(userId, currentMonth.format(MONTH_FORMATTER));
        List<Checkin> previousMonthCheckins = checkinRepository.findByUserIdAndMonthKey(userId, previousMonth.format(MONTH_FORMATTER));

        return new UserSummaryResponse(
                targetUser.id(),
                targetUser.displayNickname(),
                TimeKeyUtil.deriveWeekKey(currentWeekStart.atStartOfDay(KST_ZONE).toInstant()),
                countPresent(currentWeekCheckins),
                TimeKeyUtil.deriveWeekKey(previousWeekStart.atStartOfDay(KST_ZONE).toInstant()),
                countPresent(previousWeekCheckins),
                currentMonth.format(MONTH_FORMATTER),
                countPresent(currentMonthCheckins),
                previousMonth.format(MONTH_FORMATTER),
                countPresent(previousMonthCheckins)
        );
    }

    private MonthlyBoardMemberResponse toMonthlyBoardMember(User user, String monthKey) {
        int monthlyCount = countPresent(checkinRepository.findByUserIdAndMonthKey(user.id(), monthKey));
        return new MonthlyBoardMemberResponse(
                user.id(),
                user.displayNickname(),
                user.role(),
                monthlyCount
        );
    }

    private List<CheckinDayResponse> buildCalendarDays(List<Checkin> checkins, YearMonth targetMonth, boolean allowCheckinAction) {
        Map<String, CheckinStatus> statuses = checkins.stream()
                .collect(LinkedHashMap::new, (map, checkin) -> map.put(checkin.date(), checkin.status()), Map::putAll);

        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();
        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> new CheckinDayResponse(
                        date.format(DATE_FORMATTER),
                        statuses.getOrDefault(date.format(DATE_FORMATTER), CheckinStatus.ABSENT),
                        allowCheckinAction && date.equals(dateTimeProvider.todayKst())
                ))
                .toList();
    }

    private List<Integer> buildWeeklyCounts(List<Checkin> checkins, YearMonth targetMonth) {
        Map<LocalDate, Integer> weeklyCounts = new LinkedHashMap<>();
        LocalDate cursor = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        while (!cursor.isAfter(endDate)) {
            LocalDate weekStart = cursor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate weekEnd = weekStart.plusDays(6);
            LocalDate clampedStart = cursor;
            LocalDate clampedEnd = weekEnd.isAfter(endDate) ? endDate : weekEnd;
            int count = (int) checkins.stream()
                    .filter(checkin -> checkin.status() == CheckinStatus.PRESENT)
                    .filter(checkin -> {
                        LocalDate date = LocalDate.parse(checkin.date(), DATE_FORMATTER);
                        return !date.isBefore(clampedStart) && !date.isAfter(clampedEnd);
                    })
                    .count();
            weeklyCounts.put(clampedStart, count);
            cursor = clampedEnd.plusDays(1);
        }

        return weeklyCounts.values().stream().toList();
    }

    private int countPresent(List<Checkin> checkins) {
        return (int) checkins.stream()
                .filter(checkin -> checkin.status() == CheckinStatus.PRESENT)
                .count();
    }

    private User getActiveUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKIN_TARGET_USER_NOT_FOUND));
        if (user.status() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }
        return user;
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

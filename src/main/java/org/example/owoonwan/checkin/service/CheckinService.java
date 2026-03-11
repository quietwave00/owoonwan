package org.example.owoonwan.checkin.service;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.dto.AdminBulkCheckinRequest;
import org.example.owoonwan.checkin.dto.CheckinDayResponse;
import org.example.owoonwan.checkin.dto.CheckinPeriodResponse;
import org.example.owoonwan.checkin.dto.UserBulkCheckinRequest;
import org.example.owoonwan.checkin.repository.CheckinRepository;
import org.example.owoonwan.checkin.repository.CheckinSaveCommand;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.time.TimeKeyUtil;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserStatus;
import org.example.owoonwan.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CheckinService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CheckinRepository checkinRepository;
    private final UserRepository userRepository;
    private final KstDateTimeProvider dateTimeProvider;

    public List<Checkin> checkinDates(AuthenticatedUser authenticatedUser, UserBulkCheckinRequest request) {
        return saveDates(authenticatedUser, request, CheckinStatus.PRESENT);
    }

    public List<Checkin> cancelDates(AuthenticatedUser authenticatedUser, UserBulkCheckinRequest request) {
        return saveDates(authenticatedUser, request, CheckinStatus.ABSENT);
    }

    public Checkin checkinToday(AuthenticatedUser authenticatedUser) {
        return saveForDate(authenticatedUser.userId(), dateTimeProvider.todayKst(), CheckinStatus.PRESENT);
    }

    public Checkin cancelToday(AuthenticatedUser authenticatedUser) {
        return saveForDate(authenticatedUser.userId(), dateTimeProvider.todayKst(), CheckinStatus.ABSENT);
    }

    public List<Checkin> bulkCheckinForUsers(AdminBulkCheckinRequest request) {
        LocalDate targetDate = parseRequiredDate(request == null ? null : request.date());
        List<String> userIds = normalizeUserIds(request == null ? null : request.userIds());

        List<Checkin> saved = new ArrayList<>();
        for (String userId : userIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CHECKIN_TARGET_USER_NOT_FOUND));
            if (user.status() != UserStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
            }
            saved.add(saveForDate(userId, targetDate, CheckinStatus.PRESENT));
        }
        return saved;
    }

    public CheckinPeriodResponse getMyWeek(AuthenticatedUser authenticatedUser, String date) {
        LocalDate targetDate = parseDateOrToday(date);
        return buildWeekResponse(authenticatedUser.userId(), targetDate, authenticatedUser.userId());
    }

    public CheckinPeriodResponse getMyMonth(AuthenticatedUser authenticatedUser, String month) {
        YearMonth targetMonth = parseMonthOrCurrent(month);
        return buildMonthResponse(authenticatedUser.userId(), targetMonth, authenticatedUser.userId());
    }

    public CheckinPeriodResponse getUserMonth(String userId, String month, String viewerUserId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKIN_TARGET_USER_NOT_FOUND));
        if (targetUser.status() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }

        YearMonth targetMonth = parseMonthOrCurrent(month);
        return buildMonthResponse(userId, targetMonth, viewerUserId);
    }

    private List<Checkin> saveDates(AuthenticatedUser authenticatedUser, UserBulkCheckinRequest request, CheckinStatus status) {
        List<LocalDate> dates = normalizeDates(request);
        List<Checkin> saved = new ArrayList<>();
        for (LocalDate date : dates) {
            saved.add(saveForDate(authenticatedUser.userId(), date, status));
        }
        return saved;
    }

    private Checkin saveForDate(String userId, LocalDate targetDate, CheckinStatus status) {
        Instant checkedAt = targetDate.atStartOfDay(KST_ZONE).toInstant();
        String date = targetDate.format(DATE_FORMATTER);
        return checkinRepository.save(new CheckinSaveCommand(
                toDocumentId(userId, date),
                userId,
                date,
                TimeKeyUtil.deriveWeekKey(checkedAt),
                TimeKeyUtil.deriveMonthKey(checkedAt),
                status,
                checkedAt
        ));
    }

    private CheckinPeriodResponse buildWeekResponse(String userId, LocalDate targetDate, String viewerUserId) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKIN_TARGET_USER_NOT_FOUND));
        if (targetUser.status() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }

        LocalDate weekStart = startOfWeek(targetDate);
        LocalDate weekEnd = weekStart.plusDays(6);
        String weekKey = TimeKeyUtil.deriveWeekKey(weekStart.atStartOfDay(KST_ZONE).toInstant());
        List<Checkin> checkins = checkinRepository.findByUserIdAndDateRange(
                userId,
                weekStart.format(DATE_FORMATTER),
                weekEnd.format(DATE_FORMATTER)
        );
        return new CheckinPeriodResponse(
                userId,
                weekKey,
                weekStart.format(DATE_FORMATTER),
                weekEnd.format(DATE_FORMATTER),
                countPresentDays(checkins),
                buildDailyResponses(checkins, weekStart, weekEnd, userId.equals(viewerUserId))
        );
    }

    private CheckinPeriodResponse buildMonthResponse(String userId, YearMonth targetMonth, String viewerUserId) {
        List<Checkin> checkins = checkinRepository.findByUserIdAndMonthKey(userId, targetMonth.format(MONTH_FORMATTER));
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();
        return new CheckinPeriodResponse(
                userId,
                targetMonth.format(MONTH_FORMATTER),
                startDate.format(DATE_FORMATTER),
                endDate.format(DATE_FORMATTER),
                countPresentDays(checkins),
                buildDailyResponses(checkins, startDate, endDate, userId.equals(viewerUserId))
        );
    }

    private List<CheckinDayResponse> buildDailyResponses(
            List<Checkin> checkins,
            LocalDate startDate,
            LocalDate endDate,
            boolean allowCheckinAction
    ) {
        Map<String, CheckinStatus> statuses = checkins.stream()
                .collect(LinkedHashMap::new, (map, checkin) -> map.put(checkin.date(), checkin.status()), Map::putAll);
        LocalDate today = dateTimeProvider.todayKst();

        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> new CheckinDayResponse(
                        date.format(DATE_FORMATTER),
                        statuses.getOrDefault(date.format(DATE_FORMATTER), CheckinStatus.ABSENT),
                        allowCheckinAction && !date.isAfter(today)
                ))
                .toList();
    }

    private int countPresentDays(List<Checkin> checkins) {
        return (int) checkins.stream()
                .filter(checkin -> checkin.status() == CheckinStatus.PRESENT)
                .count();
    }

    private LocalDate parseDateOrToday(String date) {
        if (date == null || date.isBlank()) {
            return dateTimeProvider.todayKst();
        }
        return parseRequiredDate(date);
    }

    private LocalDate parseRequiredDate(String date) {
        if (date == null || date.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "date 값이 필요합니다");
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

    private LocalDate startOfWeek(LocalDate targetDate) {
        return targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private String toDocumentId(String userId, String date) {
        return userId + "_" + date.replace("-", "");
    }

    private List<String> normalizeUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "userIds 값이 필요합니다");
        }
        List<String> normalized = userIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "userIds 값이 필요합니다");
        }
        return normalized;
    }

    private List<LocalDate> normalizeDates(UserBulkCheckinRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "dates 값이 필요합니다");
        }

        List<String> rawDates = new ArrayList<>();
        if (request.date() != null && !request.date().isBlank()) {
            rawDates.add(request.date());
        }
        if (request.dates() != null) {
            rawDates.addAll(request.dates());
        }
        if (rawDates.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "dates 값이 필요합니다");
        }

        return rawDates.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .map(this::parseRequiredDate)
                .sorted()
                .toList();
    }
}

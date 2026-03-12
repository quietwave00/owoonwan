package org.example.owoonwan.title.service;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.repository.CheckinRepository;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.title.domain.TitleRules;
import org.example.owoonwan.title.dto.SpecialTitleResponse;
import org.example.owoonwan.title.dto.TitleResponse;
import org.example.owoonwan.title.dto.TitleScopeResponse;
import org.example.owoonwan.time.TimeKeyUtil;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserStatus;
import org.example.owoonwan.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TitleQueryService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CheckinRepository checkinRepository;
    private final UserRepository userRepository;
    private final TitleRuleService titleRuleService;
    private final WeeklyTitleCalculator weeklyTitleCalculator;
    private final MonthlyTitleCalculator monthlyTitleCalculator;
    private final SpecialTitleResolver specialTitleResolver;
    private final KstDateTimeProvider dateTimeProvider;

    public TitleResponse getMyCurrentWeek(AuthenticatedUser authenticatedUser) {
        LocalDate today = dateTimeProvider.todayKst();
        return buildResponse(getActiveUser(authenticatedUser.userId()), toWeekKey(today), YearMonth.from(today).format(MONTH_FORMATTER));
    }

    public TitleResponse getMyCurrentMonth(AuthenticatedUser authenticatedUser) {
        LocalDate today = dateTimeProvider.todayKst();
        return buildResponse(getActiveUser(authenticatedUser.userId()), toWeekKey(today), YearMonth.from(today).format(MONTH_FORMATTER));
    }

    public TitleResponse getUserTitles(String userId, String weekKey, String monthKey) {
        LocalDate today = dateTimeProvider.todayKst();
        String resolvedWeekKey = (weekKey == null || weekKey.isBlank()) ? toWeekKey(today) : weekKey;
        String resolvedMonthKey = (monthKey == null || monthKey.isBlank()) ? YearMonth.from(today).format(MONTH_FORMATTER) : monthKey;
        return buildResponse(getActiveUser(userId), resolvedWeekKey, resolvedMonthKey);
    }

    public TitleResponse getUserTitles(User user, String weekKey, String monthKey) {
        LocalDate today = dateTimeProvider.todayKst();
        String resolvedWeekKey = (weekKey == null || weekKey.isBlank()) ? toWeekKey(today) : weekKey;
        String resolvedMonthKey = (monthKey == null || monthKey.isBlank()) ? YearMonth.from(today).format(MONTH_FORMATTER) : monthKey;
        if (user.status() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }
        return buildResponse(user, resolvedWeekKey, resolvedMonthKey);
    }

    public TitleResponse getUserTitles(User user, String weekKey, String monthKey, int weeklyCount, int monthlyCount) {
        LocalDate today = dateTimeProvider.todayKst();
        String resolvedWeekKey = (weekKey == null || weekKey.isBlank()) ? toWeekKey(today) : weekKey;
        String resolvedMonthKey = (monthKey == null || monthKey.isBlank()) ? YearMonth.from(today).format(MONTH_FORMATTER) : monthKey;
        if (user.status() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }
        return buildResponse(user, resolvedWeekKey, resolvedMonthKey, weeklyCount, monthlyCount);
    }

    private TitleResponse buildResponse(User user, String weekKey, String monthKey) {
        List<Checkin> weeklyCheckins = checkinRepository.findByUserIdAndDateRange(
                user.id(),
                parseWeekStart(weekKey).format(DATE_FORMATTER),
                parseWeekStart(weekKey).plusDays(6).format(DATE_FORMATTER)
        );
        List<Checkin> monthlyCheckins = checkinRepository.findByUserIdAndMonthKey(user.id(), parseMonthKey(monthKey).format(MONTH_FORMATTER));

        return buildResponse(user, weekKey, monthKey, countPresent(weeklyCheckins), countPresent(monthlyCheckins));
    }

    private TitleResponse buildResponse(User user, String weekKey, String monthKey, int weeklyCount, int monthlyCount) {
        TitleRules rules = titleRuleService.getRules();
        TitleScopeResponse weekly = weeklyTitleCalculator.calculate(weeklyCount, rules);
        TitleScopeResponse monthly = monthlyTitleCalculator.calculate(monthlyCount, rules);
        SpecialTitleResponse special = specialTitleResolver.resolve(user);

        return new TitleResponse(
                user.id(),
                weekKey,
                monthKey,
                weekly,
                monthly,
                special,
                buildEffectiveBadges(weekly, monthly, special)
        );
    }

    private List<String> buildEffectiveBadges(TitleScopeResponse weekly, TitleScopeResponse monthly, SpecialTitleResponse special) {
        List<String> badges = new ArrayList<>();
        if (special.kakkdugi()) {
            badges.add("깍두기");
        }
        if (weekly.human() || monthly.human()) {
            badges.add("인간");
        }
        if (weekly.soul() || monthly.soul()) {
            badges.add("영혼");
        }
        return badges;
    }

    private int countPresent(List<Checkin> checkins) {
        return (int) checkins.stream()
                .filter(checkin -> checkin.status() == CheckinStatus.PRESENT)
                .count();
    }

    private User getActiveUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.status() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }
        return user;
    }

    private String toWeekKey(LocalDate date) {
        return TimeKeyUtil.deriveWeekKey(date.atStartOfDay(KST_ZONE).toInstant());
    }

    private LocalDate parseWeekStart(String weekKey) {
        try {
            String[] parts = weekKey.split("-W");
            int year = Integer.parseInt(parts[0]);
            int week = Integer.parseInt(parts[1]);
            WeekFields weekFields = WeekFields.ISO;
            return LocalDate.of(year, 1, 4)
                    .with(weekFields.weekOfWeekBasedYear(), week)
                    .with(weekFields.dayOfWeek(), 1);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "weekKey 형식은 yyyy-Www 이어야 합니다.");
        }
    }

    private YearMonth parseMonthKey(String monthKey) {
        try {
            return YearMonth.parse(monthKey, MONTH_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.CHECKIN_INVALID_MONTH);
        }
    }
}

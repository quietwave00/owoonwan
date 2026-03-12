package org.example.owoonwan.board.service;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.board.dto.WeeklyBoardDayResponse;
import org.example.owoonwan.board.dto.WeeklyBoardMemberResponse;
import org.example.owoonwan.board.dto.WeeklyBoardResponse;
import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.repository.CheckinRepository;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.time.TimeKeyUtil;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserStatus;
import org.example.owoonwan.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeeklyBoardService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final CheckinRepository checkinRepository;
    private final UserRepository userRepository;
    private final KstDateTimeProvider dateTimeProvider;

    public WeeklyBoardResponse getWeeklyBoard(AuthenticatedUser authenticatedUser, String date) {
        LocalDate targetDate = parseDateOrToday(date);
        LocalDate weekStart = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        String weekKey = TimeKeyUtil.deriveWeekKey(weekStart.atStartOfDay(KST_ZONE).toInstant());

        List<Checkin> weeklyCheckins = checkinRepository.findByWeekKey(weekKey);
        Map<String, Checkin> checkinsBySlot = weeklyCheckins.stream()
                .collect(LinkedHashMap::new, (map, checkin) -> map.put(checkin.userId() + "|" + checkin.date(), checkin), Map::putAll);
        List<String> presentUserIds = weeklyCheckins.stream()
                .filter(checkin -> checkin.status() == CheckinStatus.PRESENT)
                .map(Checkin::userId)
                .distinct()
                .toList();

        List<WeeklyBoardMemberResponse> members = userRepository.findByIds(presentUserIds).stream()
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .sorted(java.util.Comparator.comparing(User::displayNickname))
                .map(user -> toMemberResponse(user, authenticatedUser.userId(), weekStart, weekEnd, checkinsBySlot))
                .toList();

        return new WeeklyBoardResponse(
                weekKey,
                weekStart.format(DATE_FORMATTER),
                weekEnd.format(DATE_FORMATTER),
                members
        );
    }

    private WeeklyBoardMemberResponse toMemberResponse(
            User user,
            String viewerUserId,
            LocalDate weekStart,
            LocalDate weekEnd,
            Map<String, Checkin> checkinsBySlot
    ) {
        List<WeeklyBoardDayResponse> days = weekStart.datesUntil(weekEnd.plusDays(1))
                .map(date -> {
                    String dateKey = date.format(DATE_FORMATTER);
                    Checkin checkin = checkinsBySlot.get(user.id() + "|" + dateKey);
                    CheckinStatus status = checkin == null ? CheckinStatus.ABSENT : checkin.status();
                    boolean canCheckinAction = user.id().equals(viewerUserId) && date.equals(dateTimeProvider.todayKst());
                    return new WeeklyBoardDayResponse(dateKey, status, canCheckinAction);
                })
                .toList();

        int weeklyCount = (int) days.stream().filter(day -> day.status() == CheckinStatus.PRESENT).count();
        return new WeeklyBoardMemberResponse(
                user.id(),
                user.displayNickname(),
                user.role(),
                days,
                weeklyCount
        );
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

}

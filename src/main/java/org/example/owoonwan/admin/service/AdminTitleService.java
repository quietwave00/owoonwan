package org.example.owoonwan.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.admin.dto.AdminSpecialTitleResponse;
import org.example.owoonwan.admin.dto.AdminTitleVerificationResponse;
import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.repository.CheckinRepository;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.time.TimeKeyUtil;
import org.example.owoonwan.title.dto.TitleResponse;
import org.example.owoonwan.title.service.TitleQueryService;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserStatus;
import org.example.owoonwan.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminTitleService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UserRepository userRepository;
    private final CheckinRepository checkinRepository;
    private final TitleQueryService titleQueryService;
    private final KstDateTimeProvider dateTimeProvider;

    public AdminSpecialTitleResponse assignKakkdugi(String userId) {
        return AdminSpecialTitleResponse.from(userRepository.updateKakkdugi(userId, true));
    }

    public AdminSpecialTitleResponse revokeKakkdugi(String userId) {
        return AdminSpecialTitleResponse.from(userRepository.updateKakkdugi(userId, false));
    }

    public AdminTitleVerificationResponse verifyTitles(String weekKey, String monthKey) {
        LocalDate today = dateTimeProvider.todayKst();
        String resolvedWeekKey = (weekKey == null || weekKey.isBlank())
                ? TimeKeyUtil.deriveWeekKey(today.atStartOfDay(KST_ZONE).toInstant())
                : weekKey;
        String resolvedMonthKey = (monthKey == null || monthKey.isBlank())
                ? YearMonth.from(today).format(MONTH_FORMATTER)
                : monthKey;

        Map<String, Integer> weeklyCounts = countPresentByUserId(checkinRepository.findByWeekKey(resolvedWeekKey));
        Map<String, Integer> monthlyCounts = countPresentByUserId(checkinRepository.findByMonthKey(resolvedMonthKey));
        List<TitleResponse> titles = userRepository.findAll().stream()
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .sorted(Comparator.comparing(User::loginId))
                .map(user -> titleQueryService.getUserTitles(
                        user,
                        resolvedWeekKey,
                        resolvedMonthKey,
                        weeklyCounts.getOrDefault(user.id(), 0),
                        monthlyCounts.getOrDefault(user.id(), 0)
                ))
                .toList();

        return new AdminTitleVerificationResponse(resolvedWeekKey, resolvedMonthKey, titles);
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
}

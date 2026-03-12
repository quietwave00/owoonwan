package org.example.owoonwan.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.admin.dto.AdminSpecialTitleResponse;
import org.example.owoonwan.admin.dto.AdminTitleVerificationResponse;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTitleService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UserRepository userRepository;
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

        List<TitleResponse> titles = userRepository.findAll().stream()
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .sorted(Comparator.comparing(User::loginId))
                .map(user -> titleQueryService.getUserTitles(user, resolvedWeekKey, resolvedMonthKey))
                .toList();

        return new AdminTitleVerificationResponse(resolvedWeekKey, resolvedMonthKey, titles);
    }
}

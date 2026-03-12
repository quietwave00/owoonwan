package org.example.owoonwan.checkin.service;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.dto.AdminCheckinDateResponse;
import org.example.owoonwan.checkin.dto.AdminCheckinUserStatusResponse;
import org.example.owoonwan.checkin.repository.CheckinRepository;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserStatus;
import org.example.owoonwan.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCheckinQueryService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Collator KOREAN_COLLATOR = Collator.getInstance(Locale.KOREAN);

    private final CheckinRepository checkinRepository;
    private final UserRepository userRepository;

    public AdminCheckinDateResponse getCheckinsByDate(String date) {
        LocalDate targetDate = parseDate(date);
        String dateKey = targetDate.format(DATE_FORMATTER);
        Map<String, Checkin> checkinsByUserId = checkinRepository.findByDate(dateKey).stream()
                .collect(Collectors.toMap(Checkin::userId, Function.identity(), (left, right) -> right));

        List<AdminCheckinUserStatusResponse> users = userRepository.findAll().stream()
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .sorted(Comparator.comparing(User::displayNickname, KOREAN_COLLATOR))
                .map(user -> toResponse(user, checkinsByUserId.get(user.id())))
                .toList();

        int checkedCount = (int) users.stream().filter(AdminCheckinUserStatusResponse::checked).count();
        return new AdminCheckinDateResponse(dateKey, checkedCount, users);
    }

    private AdminCheckinUserStatusResponse toResponse(User user, Checkin checkin) {
        CheckinStatus status = checkin == null ? CheckinStatus.ABSENT : checkin.status();
        return new AdminCheckinUserStatusResponse(
                user.id(),
                user.displayNickname(),
                user.role(),
                status,
                status == CheckinStatus.PRESENT,
                checkin == null ? null : checkin.checkedAt()
        );
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "date 값이 필요합니다.");
        }
        try {
            return LocalDate.parse(date, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.CHECKIN_INVALID_DATE);
        }
    }
}

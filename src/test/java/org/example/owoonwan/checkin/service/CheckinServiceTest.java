package org.example.owoonwan.checkin.service;

import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.dto.CheckinPeriodResponse;
import org.example.owoonwan.checkin.dto.UserBulkCheckinRequest;
import org.example.owoonwan.checkin.repository.CheckinRepository;
import org.example.owoonwan.checkin.repository.CheckinSaveCommand;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserRole;
import org.example.owoonwan.user.domain.UserStatus;
import org.example.owoonwan.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckinServiceTest {

    @Test
    @DisplayName("같은 날 체크인을 여러 번 저장해도 PRESENT 상태를 유지한다")
    void shouldKeepPresentStatusWhenCheckingInRepeatedly() {
        Instant now = Instant.parse("2026-03-09T00:00:00Z");
        CheckinService service = createService(now);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("u1", "member01", "n1", UserRole.REGULAR, "token");

        Checkin first = service.checkinToday(authenticatedUser);
        Checkin repeated = service.checkinToday(authenticatedUser);

        assertEquals(CheckinStatus.PRESENT, first.status());
        assertEquals(CheckinStatus.PRESENT, repeated.status());
    }

    @Test
    @DisplayName("체크인 취소를 호출하면 오늘 상태가 ABSENT로 저장된다")
    void shouldSaveAbsentStatusWhenCancellingTodayCheckin() {
        Instant now = Instant.parse("2026-03-09T00:00:00Z");
        CheckinService service = createService(now);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("u1", "member01", "n1", UserRole.REGULAR, "token");

        service.checkinToday(authenticatedUser);
        Checkin cancelled = service.cancelToday(authenticatedUser);

        assertEquals(CheckinStatus.ABSENT, cancelled.status());
    }

    @Test
    @DisplayName("사용자 날짜 배열 체크인은 여러 날짜를 한 번에 PRESENT로 저장한다")
    void shouldBulkCheckinDatesForUser() {
        Instant now = Instant.parse("2026-03-11T00:00:00Z");
        CheckinService service = createService(now);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("u1", "member01", "n1", UserRole.REGULAR, "token");

        List<Checkin> saved = service.checkinDates(authenticatedUser, new UserBulkCheckinRequest(null, List.of("2026-03-09", "2026-03-10")));

        assertEquals(2, saved.size());
        assertEquals("2026-03-09", saved.get(0).date());
        assertEquals("2026-03-10", saved.get(1).date());
        assertEquals(CheckinStatus.PRESENT, saved.get(0).status());
    }

    @Test
    @DisplayName("사용자 날짜 배열 취소는 여러 날짜를 한 번에 ABSENT로 저장한다")
    void shouldBulkCancelDatesForUser() {
        Instant now = Instant.parse("2026-03-11T00:00:00Z");
        CheckinService service = createService(now);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("u1", "member01", "n1", UserRole.REGULAR, "token");

        service.checkinDates(authenticatedUser, new UserBulkCheckinRequest(null, List.of("2026-03-09", "2026-03-10")));
        List<Checkin> cancelled = service.cancelDates(authenticatedUser, new UserBulkCheckinRequest("2026-03-10", null));

        assertEquals(1, cancelled.size());
        assertEquals(CheckinStatus.ABSENT, cancelled.get(0).status());
        assertEquals("2026-03-10", cancelled.get(0).date());
    }

    @Test
    @DisplayName("주간 조회에서는 오늘 이전 날짜도 canCheckinAction=true로 내려간다")
    void shouldAllowPastDatesInWeeklyView() {
        Instant now = Instant.parse("2026-03-11T00:00:00Z");
        CheckinService service = createService(now);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("u1", "member01", "n1", UserRole.REGULAR, "token");

        CheckinPeriodResponse response = service.getMyWeek(authenticatedUser, "2026-03-11");

        assertEquals(true, response.days().stream().anyMatch(day -> "2026-03-09".equals(day.date()) && day.canCheckinAction()));
        assertEquals(true, response.days().stream().anyMatch(day -> "2026-03-11".equals(day.date()) && day.canCheckinAction()));
        assertEquals(false, response.days().stream().anyMatch(day -> "2026-03-15".equals(day.date()) && day.canCheckinAction()));
    }

    @Test
    @DisplayName("주간 조회에서 없는 날짜를 ABSENT로 보정한다")
    void shouldFillAbsentDaysForWeeklyView() {
        Instant now = Instant.parse("2026-03-11T00:00:00Z");
        CheckinService service = createService(now);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("u1", "member01", "n1", UserRole.REGULAR, "token");

        service.checkinToday(authenticatedUser);
        CheckinPeriodResponse response = service.getMyWeek(authenticatedUser, "2026-03-11");

        assertEquals("2026-W11", response.periodKey());
        assertEquals(7, response.days().size());
        assertEquals(1, response.presentCount());
        assertEquals(CheckinStatus.ABSENT, response.days().get(0).status());
        assertEquals(CheckinStatus.PRESENT, response.days().stream()
                .filter(day -> "2026-03-11".equals(day.date()))
                .findFirst()
                .orElseThrow()
                .status());
    }

    @Test
    @DisplayName("month 파라미터 형식이 잘못되면 예외를 던진다")
    void shouldRejectInvalidMonthFormat() {
        CheckinService service = createService(Instant.parse("2026-03-11T00:00:00Z"));
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("u1", "member01", "n1", UserRole.REGULAR, "token");

        assertThrows(BusinessException.class, () -> service.getMyMonth(authenticatedUser, "2026/03"));
    }

    private CheckinService createService(Instant now) {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.save(new User("u1", "member01", "n1", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));

        return new CheckinService(
                new InMemoryCheckinRepository(),
                userRepository,
                new KstDateTimeProvider(Clock.fixed(now, ZoneOffset.UTC))
        );
    }

    private static final class InMemoryCheckinRepository implements CheckinRepository {
        private final Map<String, Checkin> store = new HashMap<>();

        @Override
        public Checkin save(CheckinSaveCommand command) {
            Checkin updated = new Checkin(
                    command.documentId(),
                    command.userId(),
                    command.date(),
                    command.weekKey(),
                    command.monthKey(),
                    command.status(),
                    command.checkedAt()
            );
            store.put(command.documentId(), updated);
            return updated;
        }

        @Override
        public List<Checkin> findByDate(String date) {
            return store.values().stream()
                    .filter(checkin -> date.equals(checkin.date()))
                    .sorted(Comparator.comparing(Checkin::userId))
                    .toList();
        }

        @Override
        public List<Checkin> findByUserIdAndDateRange(String userId, String startDate, String endDate) {
            return store.values().stream()
                    .filter(checkin -> userId.equals(checkin.userId()))
                    .filter(checkin -> checkin.date().compareTo(startDate) >= 0 && checkin.date().compareTo(endDate) <= 0)
                    .sorted(Comparator.comparing(Checkin::date))
                    .toList();
        }

        @Override
        public List<Checkin> findByUserIdAndMonthKey(String userId, String monthKey) {
            return store.values().stream()
                    .filter(checkin -> userId.equals(checkin.userId()) && monthKey.equals(checkin.monthKey()))
                    .sorted(Comparator.comparing(Checkin::date))
                    .toList();
        }

        @Override
        public List<Checkin> findByWeekKey(String weekKey) {
            return store.values().stream()
                    .filter(checkin -> weekKey.equals(checkin.weekKey()))
                    .sorted(Comparator.comparing(Checkin::userId).thenComparing(Checkin::date))
                    .toList();
        }
    }

    private static final class InMemoryUserRepository implements UserRepository {
        private final Map<String, User> users = new HashMap<>();

        @Override
        public String create(String loginId, UserRole role, Instant now) {
            return null;
        }

        @Override
        public Optional<User> findById(String userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public Optional<User> findByLoginId(String loginId) {
            return users.values().stream()
                    .filter(user -> loginId.equals(user.loginId()))
                    .findFirst();
        }

        @Override
        public boolean existsByLoginId(String loginId) {
            return users.values().stream().anyMatch(user -> loginId.equals(user.loginId()));
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(users.values());
        }

        @Override
        public User updateRole(String userId, UserRole role) {
            return users.get(userId);
        }

        @Override
        public User updateKakkdugi(String userId, boolean kakkdugi) {
            return users.get(userId);
        }

        @Override
        public User softDelete(String userId, Instant now) {
            return users.get(userId);
        }

        @Override
        public void updateLastLoginAt(String userId, Instant now) {
        }

        void save(User user) {
            users.put(user.id(), user);
        }
    }
}

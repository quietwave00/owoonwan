package org.example.owoonwan.checkin.service;

import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.dto.CheckinPeriodResponse;
import org.example.owoonwan.checkin.dto.WeeklyBoardResponse;
import org.example.owoonwan.checkin.repository.CheckinRepository;
import org.example.owoonwan.checkin.repository.CheckinSaveCommand;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.nickname.domain.Nickname;
import org.example.owoonwan.nickname.repository.NicknameRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @DisplayName("주간 조회는 없는 날짜를 ABSENT로 보정한다")
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
    @DisplayName("주간 보드는 활성 사용자별 주간 합계와 체크인 가능 여부를 내려준다")
    void shouldBuildWeeklyBoardForActiveUsers() {
        Instant now = Instant.parse("2026-03-11T00:00:00Z");
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.save(new User("u1", "member01", "n1", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));
        userRepository.save(new User("u2", "member02", "n2", UserRole.ADMIN, UserStatus.ACTIVE, now, null, null, false, null));
        userRepository.save(new User("u3", "member03", "n3", UserRole.REGULAR, UserStatus.DELETED, now, now, null, false, null));

        InMemoryNicknameRepository nicknameRepository = new InMemoryNicknameRepository();
        nicknameRepository.save(new Nickname("n1", "가람", true, "u1", now, now));
        nicknameRepository.save(new Nickname("n2", "나래", true, "u2", now, now));
        nicknameRepository.save(new Nickname("n3", "다온", true, "u3", now, now));

        InMemoryCheckinRepository checkinRepository = new InMemoryCheckinRepository();
        CheckinService service = new CheckinService(
                checkinRepository,
                userRepository,
                nicknameRepository,
                new KstDateTimeProvider(Clock.fixed(now, ZoneOffset.UTC))
        );
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("u1", "member01", "n1", UserRole.REGULAR, "token");

        service.checkinToday(authenticatedUser);
        checkinRepository.save(new CheckinSaveCommand(
                "u2_20260310",
                "u2",
                "2026-03-10",
                "2026-W11",
                "2026-03",
                CheckinStatus.PRESENT,
                Instant.parse("2026-03-10T00:00:00Z")
        ));

        WeeklyBoardResponse response = service.getWeekBoard(authenticatedUser, "2026-03-11");

        assertEquals("2026-W11", response.weekKey());
        assertEquals(2, response.members().size());
        assertEquals("가람", response.members().get(0).nickname());
        assertEquals(1, response.members().get(0).weeklyCount());
        assertTrue(response.members().get(0).days().stream()
                .filter(day -> "2026-03-11".equals(day.date()))
                .findFirst()
                .orElseThrow()
                .canToggle());
        assertFalse(response.members().get(1).days().stream()
                .filter(day -> "2026-03-11".equals(day.date()))
                .findFirst()
                .orElseThrow()
                .canToggle());
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

        InMemoryNicknameRepository nicknameRepository = new InMemoryNicknameRepository();
        nicknameRepository.save(new Nickname("n1", "가람", true, "u1", now, now));

        return new CheckinService(
                new InMemoryCheckinRepository(),
                userRepository,
                nicknameRepository,
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

    private static final class InMemoryNicknameRepository implements NicknameRepository {
        private final Map<String, Nickname> nicknames = new HashMap<>();

        @Override
        public String create(String display, Instant now) {
            return null;
        }

        @Override
        public Optional<Nickname> findById(String nicknameId) {
            return Optional.ofNullable(nicknames.get(nicknameId));
        }

        @Override
        public List<Nickname> findAll() {
            return new ArrayList<>(nicknames.values());
        }

        @Override
        public List<Nickname> findAllActive() {
            return nicknames.values().stream().filter(Nickname::active).toList();
        }

        @Override
        public Nickname update(String nicknameId, String display, Boolean isActive, Instant now) {
            return nicknames.get(nicknameId);
        }

        @Override
        public void assignNicknameToUserFixedOnce(String nicknameId, String userId, Instant now) {
        }

        @Override
        public void clearAssignment(String userId, Instant now) {
        }

        void save(Nickname nickname) {
            nicknames.put(nickname.id(), nickname);
        }
    }
}

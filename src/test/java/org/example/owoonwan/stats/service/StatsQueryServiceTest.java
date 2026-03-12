package org.example.owoonwan.stats.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.repository.CheckinRepository;
import org.example.owoonwan.checkin.repository.CheckinSaveCommand;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.stats.dto.MonthlyBoardResponse;
import org.example.owoonwan.stats.dto.UserMonthlyCalendarResponse;
import org.example.owoonwan.stats.dto.UserSummaryResponse;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserRole;
import org.example.owoonwan.user.domain.UserStatus;
import org.example.owoonwan.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsQueryServiceTest {

    @Test
    @DisplayName("월간 보드는 active 사용자만 nicknameDisplay와 count로 정렬한다")
    void shouldBuildMonthlyBoard() {
        StatsQueryService service = createService();

        MonthlyBoardResponse response = service.getMonthlyBoard("2026-03");

        assertEquals("2026-03", response.monthKey());
        assertEquals(2, response.members().size());
        assertEquals("나리", response.members().get(0).nickname());
        assertEquals(2, response.members().get(0).monthlyCount());
        assertEquals("범수", response.members().get(1).nickname());
        assertEquals(1, response.members().get(1).monthlyCount());
    }

    @Test
    @DisplayName("멤버 월간 캘린더는 monthlyCount와 주차별 집계를 반환한다")
    void shouldBuildUserMonthlyCalendar() {
        StatsQueryService service = createService();

        UserMonthlyCalendarResponse response = service.getUserCalendar("u1", "2026-03", "u1");

        assertEquals("u1", response.uid());
        assertEquals("나리", response.nickname());
        assertEquals("2026-03", response.monthKey());
        assertEquals(31, response.days().size());
        assertEquals(2, response.monthlyCount());
        assertEquals(List.of(0, 0, 2, 0, 0, 0), response.weeklyCounts());
        assertEquals(CheckinStatus.PRESENT, response.days().stream()
                .filter(day -> "2026-03-11".equals(day.date()))
                .findFirst()
                .orElseThrow()
                .status());
    }

    @Test
    @DisplayName("summary는 현재/이전 주차와 월간 출석 수를 반환한다")
    void shouldBuildUserSummary() {
        StatsQueryService service = createService();

        UserSummaryResponse response = service.getUserSummary("u1");

        assertEquals("u1", response.uid());
        assertEquals("나리", response.nickname());
        assertEquals("2026-W11", response.currentWeekKey());
        assertEquals(2, response.currentWeekCount());
        assertEquals("2026-W10", response.previousWeekKey());
        assertEquals(0, response.previousWeekCount());
        assertEquals("2026-03", response.currentMonthKey());
        assertEquals(2, response.currentMonthCount());
        assertEquals("2026-02", response.previousMonthKey());
        assertEquals(1, response.previousMonthCount());
    }

    private StatsQueryService createService() {
        Instant now = Instant.parse("2026-03-11T00:00:00Z");

        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.save(new User("u1", "member01", "n1", "나리", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));
        userRepository.save(new User("u2", "member02", "n2", "범수", UserRole.ADMIN, UserStatus.ACTIVE, now, null, null, false, null));
        userRepository.save(new User("u3", "member03", "n3", "채린", UserRole.REGULAR, UserStatus.DELETED, now, now, null, false, null));

        InMemoryCheckinRepository checkinRepository = new InMemoryCheckinRepository();
        checkinRepository.save(new CheckinSaveCommand("u1_20260310", "u1", "2026-03-10", "2026-W11", "2026-03", CheckinStatus.PRESENT, Instant.parse("2026-03-10T00:00:00Z")));
        checkinRepository.save(new CheckinSaveCommand("u1_20260311", "u1", "2026-03-11", "2026-W11", "2026-03", CheckinStatus.PRESENT, Instant.parse("2026-03-11T00:00:00Z")));
        checkinRepository.save(new CheckinSaveCommand("u2_20260309", "u2", "2026-03-09", "2026-W11", "2026-03", CheckinStatus.PRESENT, Instant.parse("2026-03-09T00:00:00Z")));
        checkinRepository.save(new CheckinSaveCommand("u1_20260215", "u1", "2026-02-15", "2026-W07", "2026-02", CheckinStatus.PRESENT, Instant.parse("2026-02-15T00:00:00Z")));

        return new StatsQueryService(
                checkinRepository,
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
            return users.values().stream().filter(user -> loginId.equals(user.loginId())).findFirst();
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

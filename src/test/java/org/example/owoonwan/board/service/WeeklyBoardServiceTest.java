package org.example.owoonwan.board.service;

import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.board.dto.WeeklyBoardResponse;
import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.repository.CheckinRepository;
import org.example.owoonwan.checkin.repository.CheckinSaveCommand;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyBoardServiceTest {

    @Test
    @DisplayName("weekly board includes only active users with PRESENT checkins in the week")
    void shouldBuildWeeklyBoardForPresentUsers() {
        Instant now = Instant.parse("2026-03-11T00:00:00Z");
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.save(new User("u1", "member01", "n1", "sari", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));
        userRepository.save(new User("u2", "member02", "n2", "beom", UserRole.ADMIN, UserStatus.ACTIVE, now, null, null, false, null));
        userRepository.save(new User("u3", "member03", "n3", "chaerin", UserRole.REGULAR, UserStatus.DELETED, now, now, null, false, null));
        userRepository.save(new User("u4", "member04", "n4", "doyoon", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));

        InMemoryCheckinRepository checkinRepository = new InMemoryCheckinRepository();
        checkinRepository.save(new CheckinSaveCommand(
                "u1_20260311",
                "u1",
                "2026-03-11",
                "2026-W11",
                "2026-03",
                CheckinStatus.PRESENT,
                now
        ));
        checkinRepository.save(new CheckinSaveCommand(
                "u2_20260310",
                "u2",
                "2026-03-10",
                "2026-W11",
                "2026-03",
                CheckinStatus.PRESENT,
                Instant.parse("2026-03-10T00:00:00Z")
        ));
        checkinRepository.save(new CheckinSaveCommand(
                "u3_20260312",
                "u3",
                "2026-03-12",
                "2026-W11",
                "2026-03",
                CheckinStatus.PRESENT,
                Instant.parse("2026-03-12T00:00:00Z")
        ));
        checkinRepository.save(new CheckinSaveCommand(
                "u4_20260312",
                "u4",
                "2026-03-12",
                "2026-W11",
                "2026-03",
                CheckinStatus.ABSENT,
                Instant.parse("2026-03-12T00:00:00Z")
        ));

        WeeklyBoardService service = new WeeklyBoardService(
                checkinRepository,
                userRepository,
                new KstDateTimeProvider(Clock.fixed(now, ZoneOffset.UTC))
        );
        AuthenticatedUser authenticatedUser = new AuthenticatedUser("u1", "member01", "n1", "sari", UserRole.REGULAR, "token");

        WeeklyBoardResponse response = service.getWeeklyBoard(authenticatedUser, "2026-03-11");

        assertEquals("2026-W11", response.weekKey());
        assertEquals("2026-03-09", response.weekStartDate());
        assertEquals("2026-03-15", response.weekEndDate());
        assertEquals(2, response.members().size());
        assertEquals("beom", response.members().get(0).nickname());
        assertEquals("sari", response.members().get(1).nickname());
        assertEquals(1, response.members().get(1).weeklyCount());
        assertTrue(response.members().get(1).days().stream()
                .filter(day -> "2026-03-11".equals(day.date()))
                .findFirst()
                .orElseThrow()
                .canCheckinAction());
        assertFalse(response.members().get(0).days().stream()
                .filter(day -> "2026-03-11".equals(day.date()))
                .findFirst()
                .orElseThrow()
                .canCheckinAction());
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

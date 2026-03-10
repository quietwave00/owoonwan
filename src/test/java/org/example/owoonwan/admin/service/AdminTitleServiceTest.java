package org.example.owoonwan.admin.service;

import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.repository.CheckinRepository;
import org.example.owoonwan.checkin.repository.CheckinSaveCommand;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.title.domain.TitleRules;
import org.example.owoonwan.title.dto.TitleResponse;
import org.example.owoonwan.title.repository.TitleRuleRepository;
import org.example.owoonwan.title.service.MonthlyTitleCalculator;
import org.example.owoonwan.title.service.SpecialTitleResolver;
import org.example.owoonwan.title.service.TitleQueryService;
import org.example.owoonwan.title.service.TitleRuleService;
import org.example.owoonwan.title.service.WeeklyTitleCalculator;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserRole;
import org.example.owoonwan.user.domain.UserStatus;
import org.example.owoonwan.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminTitleServiceTest {

    @Test
    @DisplayName("관리자가 깍두기를 부여하고 해제할 수 있다")
    void shouldAssignAndRevokeKakkdugi() {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        Instant now = Instant.parse("2026-03-11T00:00:00Z");
        userRepository.save(new User("u1", "member01", "n1", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));

        AdminTitleService service = new AdminTitleService(
                userRepository,
                createTitleQueryService(userRepository, now),
                new KstDateTimeProvider(Clock.fixed(now, ZoneOffset.UTC))
        );

        assertTrue(service.assignKakkdugi("u1").kakkdugi());
        assertFalse(service.revokeKakkdugi("u1").kakkdugi());
    }

    @Test
    @DisplayName("관리자 타이틀 검증 조회는 활성 사용자만 포함한다")
    void shouldVerifyTitlesForActiveUsers() {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        Instant now = Instant.parse("2026-03-11T00:00:00Z");
        userRepository.save(new User("u1", "member01", "n1", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));
        userRepository.save(new User("u2", "member02", "n2", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, true, null));
        userRepository.save(new User("u3", "member03", "n3", UserRole.REGULAR, UserStatus.DELETED, now, now, null, false, null));

        AdminTitleService service = new AdminTitleService(
                userRepository,
                createTitleQueryService(userRepository, now),
                new KstDateTimeProvider(Clock.fixed(now, ZoneOffset.UTC))
        );

        List<TitleResponse> titles = service.verifyTitles("2026-W11", "2026-03").titles();

        assertEquals(2, titles.size());
        assertEquals("u1", titles.get(0).uid());
        assertEquals("u2", titles.get(1).uid());
    }

    private TitleQueryService createTitleQueryService(UserRepository userRepository, Instant now) {
        InMemoryCheckinRepository checkinRepository = new InMemoryCheckinRepository();
        checkinRepository.save(new CheckinSaveCommand("u1_20260309", "u1", "2026-03-09", "2026-W11", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u1_20260310", "u1", "2026-03-10", "2026-W11", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u1_20260311", "u1", "2026-03-11", "2026-W11", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u2_20260310", "u2", "2026-03-10", "2026-W11", "2026-03", CheckinStatus.ABSENT, now));

        return new TitleQueryService(
                checkinRepository,
                userRepository,
                new TitleRuleService(new SingleObjectProvider<>(new InMemoryTitleRuleRepository())),
                new WeeklyTitleCalculator(),
                new MonthlyTitleCalculator(),
                new SpecialTitleResolver(),
                new KstDateTimeProvider(Clock.fixed(now, ZoneOffset.UTC))
        );
    }

    private static final class InMemoryTitleRuleRepository implements TitleRuleRepository {
        @Override
        public Optional<TitleRules> findRules() {
            return Optional.of(new TitleRules(3, 12));
        }
    }

    private static final class InMemoryCheckinRepository implements CheckinRepository {
        private final Map<String, Checkin> store = new HashMap<>();

        @Override
        public Checkin save(CheckinSaveCommand command) {
            Checkin checkin = new Checkin(
                    command.documentId(),
                    command.userId(),
                    command.date(),
                    command.weekKey(),
                    command.monthKey(),
                    command.status(),
                    command.checkedAt()
            );
            store.put(command.documentId(), checkin);
            return checkin;
        }

        @Override
        public List<Checkin> findByDate(String date) {
            return store.values().stream()
                    .filter(checkin -> date.equals(checkin.date()))
                    .toList();
        }

        @Override
        public List<Checkin> findByUserIdAndDateRange(String userId, String startDate, String endDate) {
            return store.values().stream()
                    .filter(checkin -> userId.equals(checkin.userId()))
                    .filter(checkin -> checkin.date().compareTo(startDate) >= 0 && checkin.date().compareTo(endDate) <= 0)
                    .toList();
        }

        @Override
        public List<Checkin> findByUserIdAndMonthKey(String userId, String monthKey) {
            return store.values().stream()
                    .filter(checkin -> userId.equals(checkin.userId()) && monthKey.equals(checkin.monthKey()))
                    .toList();
        }

        @Override
        public List<Checkin> findByWeekKey(String weekKey) {
            return List.of();
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
            User current = users.get(userId);
            if (current == null) {
                return null;
            }
            User updated = new User(
                    current.id(),
                    current.loginId(),
                    current.nicknameId(),
                    current.role(),
                    current.status(),
                    current.createdAt(),
                    current.deletedAt(),
                    current.lastLoginAt(),
                    kakkdugi,
                    current.pledgeId()
            );
            users.put(userId, updated);
            return updated;
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

    private static final class SingleObjectProvider<T> implements ObjectProvider<T> {
        private final T value;

        private SingleObjectProvider(T value) {
            this.value = value;
        }

        @Override
        public T getObject(Object... args) {
            return value;
        }

        @Override
        public T getIfAvailable() {
            return value;
        }

        @Override
        public T getIfUnique() {
            return value;
        }

        @Override
        public T getObject() {
            return value;
        }
    }
}

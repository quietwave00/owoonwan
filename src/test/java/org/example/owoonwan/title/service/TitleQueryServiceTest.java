package org.example.owoonwan.title.service;

import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.repository.CheckinRepository;
import org.example.owoonwan.checkin.repository.CheckinSaveCommand;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.title.domain.TitleRules;
import org.example.owoonwan.title.dto.TitleResponse;
import org.example.owoonwan.title.repository.TitleRuleRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitleQueryServiceTest {

    @Test
    @DisplayName("?꾩옱 二쇨컙怨??붽컙 異쒖꽍 ?섎줈 ?멸컙怨??곹샎 ??댄???怨꾩궛?쒕떎")
    void shouldCalculateWeeklyAndMonthlyTitles() {
        TitleQueryService service = createService();

        TitleResponse response = service.getMyCurrentWeek(new AuthenticatedUser("u1", "member01", "n1", UserRole.REGULAR, "token"));

        assertEquals("u1", response.uid());
        assertEquals("2026-W11", response.weekKey());
        assertEquals("2026-03", response.monthKey());
        assertTrue(response.weekly().human());
        assertFalse(response.weekly().soul());
        assertTrue(response.monthly().human());
        assertFalse(response.monthly().soul());
        assertEquals(1, response.effectiveBadges().size());
    }

    @Test
    @DisplayName("源띾몢湲??ъ슜?먮뒗 special title???곗꽑 ?몄텧?쒕떎")
    void shouldIncludeKakkdugiAsSpecialTitle() {
        TitleQueryService service = createService();

        TitleResponse response = service.getUserTitles("u2", "2026-W11", "2026-03");

        assertTrue(response.special().kakkdugi());
        assertFalse(response.monthly().human());
        assertTrue(response.monthly().soul());
        assertEquals(2, response.effectiveBadges().size());
    }

    private TitleQueryService createService() {
        Instant now = Instant.parse("2026-03-11T00:00:00Z");
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.save(new User("u1", "member01", "n1", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));
        userRepository.save(new User("u2", "member02", "n2", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, true, null));

        InMemoryCheckinRepository checkinRepository = new InMemoryCheckinRepository();
        checkinRepository.save(new CheckinSaveCommand("u1_20260309", "u1", "2026-03-09", "2026-W11", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u1_20260310", "u1", "2026-03-10", "2026-W11", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u1_20260311", "u1", "2026-03-11", "2026-W11", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u1_20260312", "u1", "2026-03-12", "2026-W11", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u1_20260313", "u1", "2026-03-13", "2026-W11", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u1_20260314", "u1", "2026-03-14", "2026-W11", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u1_20260315", "u1", "2026-03-15", "2026-W11", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u1_20260301", "u1", "2026-03-01", "2026-W09", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u1_20260302", "u1", "2026-03-02", "2026-W10", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u1_20260303", "u1", "2026-03-03", "2026-W10", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u1_20260304", "u1", "2026-03-04", "2026-W10", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u1_20260305", "u1", "2026-03-05", "2026-W10", "2026-03", CheckinStatus.PRESENT, now));
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
            return Optional.empty();
        }

        @Override
        public boolean existsByLoginId(String loginId) {
            return false;
        }

        @Override
        public List<User> findAll() {
            return List.copyOf(users.values());
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


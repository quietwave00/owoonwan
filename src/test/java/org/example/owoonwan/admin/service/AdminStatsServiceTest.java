package org.example.owoonwan.admin.service;

import org.example.owoonwan.admin.dto.AdminMonthlyStatsResponse;
import org.example.owoonwan.admin.dto.AdminWeeklyStatsResponse;
import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.repository.CheckinRepository;
import org.example.owoonwan.checkin.repository.CheckinSaveCommand;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.title.domain.TitleRules;
import org.example.owoonwan.title.repository.TitleRuleRepository;
import org.example.owoonwan.title.service.MonthlyTitleCalculator;
import org.example.owoonwan.title.service.TitleBadgeAssembler;
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

class AdminStatsServiceTest {

    @Test
    @DisplayName("주간 관리자 통계는 일자 그리드 없이 PRESENT 체크인만 집계한다")
    void shouldBuildWeeklyAdminStats() {
        Instant now = Instant.parse("2026-03-11T00:00:00Z");
        AdminStatsService service = createService(now);

        AdminWeeklyStatsResponse response = service.getWeeklyStats("2026-03-11");

        assertEquals("2026-W11", response.weekKey());
        assertEquals("2026-03-09", response.weekStartDate());
        assertEquals("2026-03-15", response.weekEndDate());
        assertEquals(2, response.members().size());
        assertEquals("나리", response.members().get(0).nickname());
        assertEquals(2, response.members().get(0).count());
        assertEquals(List.of("영혼"), response.members().get(0).badges());
        assertEquals("범수", response.members().get(1).nickname());
        assertEquals(1, response.members().get(1).count());
        assertEquals(List.of("깍두기", "영혼"), response.members().get(1).badges());
    }

    @Test
    @DisplayName("월간 관리자 통계는 월 기준으로 PRESENT 체크인을 집계한다")
    void shouldBuildMonthlyAdminStats() {
        Instant now = Instant.parse("2026-03-11T00:00:00Z");
        AdminStatsService service = createService(now);

        AdminMonthlyStatsResponse response = service.getMonthlyStats("2026-03");

        assertEquals("2026-03", response.monthKey());
        assertEquals(2, response.members().size());
        assertEquals("나리", response.members().get(0).nickname());
        assertEquals(2, response.members().get(0).count());
        assertEquals("범수", response.members().get(1).nickname());
        assertEquals(1, response.members().get(1).count());
    }

    private AdminStatsService createService(Instant now) {
        InMemoryCheckinRepository checkinRepository = new InMemoryCheckinRepository();
        checkinRepository.save(new CheckinSaveCommand("u1_20260310", "u1", "2026-03-10", "2026-W11", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u1_20260311", "u1", "2026-03-11", "2026-W11", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u2_20260310", "u2", "2026-03-10", "2026-W11", "2026-03", CheckinStatus.PRESENT, now));
        checkinRepository.save(new CheckinSaveCommand("u3_20260310", "u3", "2026-03-10", "2026-W11", "2026-03", CheckinStatus.ABSENT, now));

        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.save(new User("u1", "member01", "n1", "나리", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));
        userRepository.save(new User("u2", "member02", "n2", "범수", UserRole.ADMIN, UserStatus.ACTIVE, now, null, null, true, null));
        userRepository.save(new User("u3", "member03", "n3", "채린", UserRole.REGULAR, UserStatus.DELETED, now, now, null, false, null));

        return new AdminStatsService(
                checkinRepository,
                userRepository,
                new TitleBadgeAssembler(
                        new TitleRuleService(new SingleObjectProvider<>(new InMemoryTitleRuleRepository())),
                        new WeeklyTitleCalculator(),
                        new MonthlyTitleCalculator()
                ),
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
        public List<Checkin> findByDate(String date) {
            return List.of();
        }

        @Override
        public List<Checkin> findByUserIdAndDateRange(String userId, String startDate, String endDate) {
            return List.of();
        }

        @Override
        public List<Checkin> findByUserIdAndMonthKey(String userId, String monthKey) {
            return List.of();
        }

        @Override
        public List<Checkin> findByWeekKey(String weekKey) {
            return store.values().stream().filter(checkin -> weekKey.equals(checkin.weekKey())).toList();
        }

        @Override
        public List<Checkin> findByMonthKey(String monthKey) {
            return store.values().stream().filter(checkin -> monthKey.equals(checkin.monthKey())).toList();
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

package org.example.owoonwan.checkin.service;

import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.dto.AdminBulkCheckinRequest;
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

class AdminCheckinServiceTest {

    @Test
    @DisplayName("관리자는 특정 날짜에 여러 사용자의 체크인을 일괄 저장할 수 있다")
    void shouldBulkCheckinUsersForSpecificDate() {
        CheckinService service = createService();

        List<Checkin> saved = service.bulkCheckinForUsers(new AdminBulkCheckinRequest("2026-03-02", List.of("u1", "u2")));

        assertEquals(2, saved.size());
        assertEquals("2026-03-02", saved.get(0).date());
        assertEquals(CheckinStatus.PRESENT, saved.get(0).status());
        assertEquals("2026-W10", saved.get(0).weekKey());
        assertEquals("2026-03", saved.get(0).monthKey());
    }

    @Test
    @DisplayName("중복된 사용자 아이디는 한 번만 처리한다")
    void shouldDeduplicateUserIds() {
        CheckinService service = createService();

        List<Checkin> saved = service.bulkCheckinForUsers(new AdminBulkCheckinRequest("2026-03-02", List.of("u1", "u1", "u2")));

        assertEquals(2, saved.size());
    }

    @Test
    @DisplayName("날짜가 없으면 관리자 일괄 체크인을 거부한다")
    void shouldRejectMissingDate() {
        CheckinService service = createService();

        assertThrows(BusinessException.class,
                () -> service.bulkCheckinForUsers(new AdminBulkCheckinRequest(null, List.of("u1"))));
    }

    private CheckinService createService() {
        Instant now = Instant.parse("2026-03-09T00:00:00Z");
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.save(new User("u1", "member01", "n1", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));
        userRepository.save(new User("u2", "member02", "n2", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));

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

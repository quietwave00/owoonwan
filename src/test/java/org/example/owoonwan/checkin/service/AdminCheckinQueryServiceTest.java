package org.example.owoonwan.checkin.service;

import org.example.owoonwan.checkin.domain.Checkin;
import org.example.owoonwan.checkin.domain.CheckinStatus;
import org.example.owoonwan.checkin.dto.AdminCheckinDateResponse;
import org.example.owoonwan.checkin.repository.CheckinRepository;
import org.example.owoonwan.checkin.repository.CheckinSaveCommand;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.nickname.domain.Nickname;
import org.example.owoonwan.nickname.repository.NicknameRepository;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserRole;
import org.example.owoonwan.user.domain.UserStatus;
import org.example.owoonwan.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminCheckinQueryServiceTest {

    @Test
    @DisplayName("관리자 일자 조회는 활성 사용자와 체크인 상태를 함께 반환한다")
    void shouldReturnUsersWithCheckinStatusForDate() {
        Instant now = Instant.parse("2026-03-09T00:00:00Z");
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.save(new User("u1", "member01", "n1", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));
        userRepository.save(new User("u2", "member02", "n2", UserRole.ADMIN, UserStatus.ACTIVE, now, null, null, false, null));
        userRepository.save(new User("u3", "member03", "n3", UserRole.REGULAR, UserStatus.DELETED, now, now, null, false, null));

        InMemoryNicknameRepository nicknameRepository = new InMemoryNicknameRepository();
        nicknameRepository.save(new Nickname("n1", "하리", true, "u1", now, now));
        nicknameRepository.save(new Nickname("n2", "범수", true, "u2", now, now));
        nicknameRepository.save(new Nickname("n3", "채린", true, "u3", now, now));

        InMemoryCheckinRepository checkinRepository = new InMemoryCheckinRepository();
        checkinRepository.save(new CheckinSaveCommand("u1_20260302", "u1", "2026-03-02", "2026-W10", "2026-03", CheckinStatus.PRESENT, now));

        AdminCheckinQueryService service = new AdminCheckinQueryService(checkinRepository, userRepository, nicknameRepository);

        AdminCheckinDateResponse response = service.getCheckinsByDate("2026-03-02");

        assertEquals("2026-03-02", response.date());
        assertEquals(1, response.checkedCount());
        assertEquals(2, response.users().size());
        assertEquals("범수", response.users().get(0).nickname());
        assertEquals(CheckinStatus.ABSENT, response.users().get(0).status());
        assertEquals("하리", response.users().get(1).nickname());
        assertEquals(CheckinStatus.PRESENT, response.users().get(1).status());
    }

    @Test
    @DisplayName("날짜 형식이 잘못되면 관리자 일자 조회를 거부한다")
    void shouldRejectInvalidDate() {
        AdminCheckinQueryService service = new AdminCheckinQueryService(
                new InMemoryCheckinRepository(),
                new InMemoryUserRepository(),
                new InMemoryNicknameRepository()
        );

        assertThrows(BusinessException.class, () -> service.getCheckinsByDate("2026/03/02"));
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
                    .sorted(Comparator.comparing(Checkin::userId))
                    .toList();
        }

        @Override
        public List<Checkin> findByUserIdAndMonthKey(String userId, String monthKey) {
            return List.of();
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

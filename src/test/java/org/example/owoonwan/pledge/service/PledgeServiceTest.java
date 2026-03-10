package org.example.owoonwan.pledge.service;

import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.nickname.domain.Nickname;
import org.example.owoonwan.nickname.repository.NicknameRepository;
import org.example.owoonwan.pledge.domain.Pledge;
import org.example.owoonwan.pledge.dto.PledgeResponse;
import org.example.owoonwan.pledge.dto.PledgeUpdateRequest;
import org.example.owoonwan.pledge.repository.PledgeRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PledgeServiceTest {

    @Test
    @DisplayName("내 다짐이 없으면 빈 문자열과 version 0을 반환한다")
    void shouldReturnEmptyPledgeWhenMissing() {
        PledgeService service = createService();

        PledgeResponse response = service.getMyPledge(authenticatedUser("u1"));

        assertEquals("u1", response.uid());
        assertEquals("하리", response.nickname());
        assertEquals("", response.text());
        assertEquals(0, response.version());
        assertNull(response.updatedAt());
    }

    @Test
    @DisplayName("내 다짐 저장 시 trim 후 version을 증가시킨다")
    void shouldTrimTextAndIncrementVersionWhenUpdatingPledge() {
        PledgeService service = createService();

        PledgeResponse created = service.upsertMyPledge(authenticatedUser("u1"), new PledgeUpdateRequest("  올해는 꾸준히 운동하기  "));
        PledgeResponse updated = service.upsertMyPledge(authenticatedUser("u1"), new PledgeUpdateRequest("매일 30분 걷기"));

        assertEquals("올해는 꾸준히 운동하기", created.text());
        assertEquals(1, created.version());
        assertEquals("매일 30분 걷기", updated.text());
        assertEquals(2, updated.version());
    }

    @Test
    @DisplayName("전체 다짐 목록은 활성 사용자 기준 닉네임 순으로 내려준다")
    void shouldListPledgesForActiveUsersInNicknameOrder() {
        PledgeService service = createService();
        service.upsertMyPledge(authenticatedUser("u2"), new PledgeUpdateRequest("다짐 B"));

        List<PledgeResponse> responses = service.listPledges("u1");

        assertEquals(2, responses.size());
        assertEquals("범수", responses.get(0).nickname());
        assertEquals("다짐 B", responses.get(0).text());
        assertEquals("하리", responses.get(1).nickname());
        assertEquals(true, responses.get(1).mine());
        assertEquals("", responses.get(1).text());
    }

    @Test
    @DisplayName("관리자 삭제 후에는 다시 빈 다짐 상태가 된다")
    void shouldDeletePledge() {
        PledgeService service = createService();
        service.upsertMyPledge(authenticatedUser("u1"), new PledgeUpdateRequest("삭제될 다짐"));

        service.deletePledge("u1");

        PledgeResponse response = service.getMyPledge(authenticatedUser("u1"));
        assertEquals("", response.text());
        assertEquals(0, response.version());
    }

    @Test
    @DisplayName("text가 null이면 저장을 거부한다")
    void shouldRejectNullText() {
        PledgeService service = createService();

        assertThrows(BusinessException.class, () -> service.upsertMyPledge(authenticatedUser("u1"), new PledgeUpdateRequest(null)));
    }

    private PledgeService createService() {
        Instant now = Instant.parse("2026-03-10T00:00:00Z");
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.save(new User("u1", "member01", "n1", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));
        userRepository.save(new User("u2", "member02", "n2", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));
        userRepository.save(new User("u3", "member03", "n3", UserRole.REGULAR, UserStatus.DELETED, now, now, null, false, null));

        InMemoryNicknameRepository nicknameRepository = new InMemoryNicknameRepository();
        nicknameRepository.save(new Nickname("n1", "하리", true, "u1", now, now));
        nicknameRepository.save(new Nickname("n2", "범수", true, "u2", now, now));
        nicknameRepository.save(new Nickname("n3", "채린", true, "u3", now, now));

        return new PledgeService(
                new InMemoryPledgeRepository(),
                userRepository,
                nicknameRepository,
                new KstDateTimeProvider(Clock.fixed(now, ZoneOffset.UTC))
        );
    }

    private AuthenticatedUser authenticatedUser(String userId) {
        return new AuthenticatedUser(userId, "login-" + userId, "nickname-" + userId, UserRole.REGULAR, "token");
    }

    private static final class InMemoryPledgeRepository implements PledgeRepository {
        private final Map<String, Pledge> store = new HashMap<>();

        @Override
        public Optional<Pledge> findByUserId(String userId) {
            return Optional.ofNullable(store.get(userId));
        }

        @Override
        public List<Pledge> findAll() {
            return store.values().stream()
                    .sorted(Comparator.comparing(Pledge::userId))
                    .toList();
        }

        @Override
        public Pledge save(String userId, String text, Instant now) {
            int nextVersion = store.containsKey(userId) ? store.get(userId).version() + 1 : 1;
            Pledge pledge = new Pledge(userId, text, now, nextVersion);
            store.put(userId, pledge);
            return pledge;
        }

        @Override
        public void deleteByUserId(String userId) {
            store.remove(userId);
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

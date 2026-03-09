package org.example.owoonwan.user.service;

import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.nickname.domain.Nickname;
import org.example.owoonwan.nickname.repository.NicknameRepository;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserRole;
import org.example.owoonwan.user.domain.UserStatus;
import org.example.owoonwan.user.dto.AdminCreateUserRequest;
import org.example.owoonwan.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UserAdminServiceTest {

    @Test
    @DisplayName("역할 없이 사용자 생성 시 기본 역할은 REGULAR로 저장된다")
    void shouldDefaultRoleToRegularWhenMissing() {
        Instant now = Instant.parse("2026-03-06T00:00:00Z");
        FakeUserRepository userRepository = new FakeUserRepository();
        FakeNicknameRepository nicknameRepository = new FakeNicknameRepository();
        UserAdminService service = new UserAdminService(
                userRepository,
                nicknameRepository,
                new KstDateTimeProvider(Clock.fixed(now, ZoneOffset.UTC))
        );

        User user = service.createUser(new AdminCreateUserRequest("User.Test", null));

        assertEquals("user.test", user.loginId());
        assertEquals(UserRole.REGULAR, user.role());
        assertFalse(user.kakkdugi());
    }

    private static final class FakeUserRepository implements UserRepository {
        private final Map<String, User> users = new HashMap<>();

        @Override
        public String create(String loginId, UserRole role, Instant now) {
            String id = "u-1";
            users.put(id, new User(id, loginId, null, role, UserStatus.ACTIVE, now, null, null, false, null));
            return id;
        }

        @Override
        public Optional<User> findById(String userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public boolean existsByLoginId(String loginId) {
            return users.values().stream().anyMatch(user -> loginId.equals(user.loginId()));
        }

        @Override
        public List<User> findAll() {
            return users.values().stream().toList();
        }

        @Override
        public User updateRole(String userId, UserRole role) {
            return users.get(userId);
        }

        @Override
        public User softDelete(String userId, Instant now) {
            return users.get(userId);
        }

    }

    private static final class FakeNicknameRepository implements NicknameRepository {

        @Override
        public String create(String display, Instant now) {
            return null;
        }

        @Override
        public Optional<Nickname> findById(String nicknameId) {
            return Optional.empty();
        }

        @Override
        public List<Nickname> findAll() {
            return List.of();
        }

        @Override
        public List<Nickname> findAllActive() {
            return List.of();
        }

        @Override
        public Nickname update(String nicknameId, String display, Boolean isActive, Instant now) {
            return null;
        }

        @Override
        public void assignNicknameToUserFixedOnce(String nicknameId, String userId, Instant now) {
        }

        @Override
        public void clearAssignment(String userId, Instant now) {
        }
    }
}

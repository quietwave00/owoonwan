package org.example.owoonwan.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.owoonwan.auth.config.AuthTokenProperties;
import org.example.owoonwan.auth.dto.AuthLoginRequest;
import org.example.owoonwan.auth.dto.AuthLoginResponse;
import org.example.owoonwan.auth.dto.AuthMeResponse;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
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
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    @Test
    @DisplayName("loginId로 로그인하면 토큰을 발급한다")
    void shouldIssueLongLivedTokenWhenLoginSucceeds() {
        Instant now = Instant.parse("2026-03-09T00:00:00Z");
        FakeUserRepository userRepository = new FakeUserRepository();
        userRepository.add(new User("u1", "member01", "nick-1", "test", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));
        AuthService authService = new AuthService(
                userRepository,
                authTokenService("test-secret", 3650),
                new KstDateTimeProvider(Clock.fixed(now, ZoneOffset.UTC))
        );

        AuthLoginResponse response = authService.login(new AuthLoginRequest("member01"));
        AuthenticatedUser authenticatedUser = authService.authenticate("Bearer " + response.sessionToken());

        assertEquals("u1", response.uid());
        assertEquals("member01", response.loginId());
        assertEquals("nick-1", response.nicknameId());
        assertEquals("test", response.nicknameDisplay());
        assertEquals(Instant.parse("2036-03-06T00:00:00Z"), response.expiresAt());
        assertEquals("u1", authenticatedUser.userId());
        assertEquals(UserRole.REGULAR, authenticatedUser.role());
    }

    @Test
    @DisplayName("닉네임이 선택되지 않은 사용자는 로그인할 수 없다")
    void shouldRejectWhenNicknameNotSelected() {
        Instant now = Instant.parse("2026-03-09T00:00:00Z");
        FakeUserRepository userRepository = new FakeUserRepository();
        userRepository.add(new User("u1", "member01", null, UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));
        AuthService authService = new AuthService(
                userRepository,
                authTokenService("test-secret", 3650),
                new KstDateTimeProvider(Clock.fixed(now, ZoneOffset.UTC))
        );

        assertThrows(BusinessException.class, () -> authService.login(new AuthLoginRequest("member01")));
    }

    @Test
    @DisplayName("인증 정보는 토큰 만료 시간을 그대로 반환한다")
    void shouldReturnMeFromAuthenticatedUser() {
        Instant now = Instant.parse("2026-03-09T00:00:00Z");
        AuthService authService = new AuthService(
                new FakeUserRepository(),
                authTokenService("test-secret", 3650),
                new KstDateTimeProvider(Clock.fixed(now, ZoneOffset.UTC))
        );

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                "u1",
                "member01",
                "nick-1",
                "test",
                UserRole.REGULAR,
                "token-0",
                now.plus(1, ChronoUnit.DAYS)
        );

        AuthMeResponse response = authService.me(authenticatedUser);

        assertEquals(now.plus(1, ChronoUnit.DAYS), response.expiresAt());
    }

    private AuthTokenService authTokenService(String secret, long maxAgeDays) {
        AuthTokenProperties properties = new AuthTokenProperties();
        properties.setSecret(secret);
        properties.setMaxAgeDays(maxAgeDays);
        return new AuthTokenService(properties, new ObjectMapper());
    }

    private static final class FakeUserRepository implements UserRepository {
        private final Map<String, User> usersById = new HashMap<>();

        @Override
        public String create(String loginId, UserRole role, Instant now) {
            return null;
        }

        @Override
        public Optional<User> findById(String userId) {
            return Optional.ofNullable(usersById.get(userId));
        }

        @Override
        public Optional<User> findByLoginId(String loginId) {
            return usersById.values().stream()
                    .filter(user -> loginId.equals(user.loginId()))
                    .findFirst();
        }

        @Override
        public boolean existsByLoginId(String loginId) {
            return usersById.values().stream().anyMatch(user -> loginId.equals(user.loginId()));
        }

        @Override
        public List<User> findAll() {
            return usersById.values().stream().toList();
        }

        @Override
        public User updateRole(String userId, UserRole role) {
            return usersById.get(userId);
        }

        @Override
        public User updateKakkdugi(String userId, boolean kakkdugi) {
            return usersById.get(userId);
        }

        @Override
        public User softDelete(String userId, Instant now) {
            return usersById.get(userId);
        }

        @Override
        public void updateLastLoginAt(String userId, Instant now) {
            User user = usersById.get(userId);
            if (user == null) {
                return;
            }
            usersById.put(userId, new User(
                    user.id(),
                    user.loginId(),
                    user.nicknameId(),
                    user.nicknameDisplay(),
                    user.role(),
                    user.status(),
                    user.createdAt(),
                    user.deletedAt(),
                    now,
                    user.kakkdugi(),
                    user.pledgeId()
            ));
        }

        void add(User user) {
            usersById.put(user.id(), user);
        }
    }
}

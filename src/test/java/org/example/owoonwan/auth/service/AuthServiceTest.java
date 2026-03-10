package org.example.owoonwan.auth.service;

import org.example.owoonwan.auth.dto.AuthLoginRequest;
import org.example.owoonwan.auth.dto.AuthLoginResponse;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.session.domain.Session;
import org.example.owoonwan.session.repository.LoginLockRepository;
import org.example.owoonwan.session.repository.SessionRepository;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserRole;
import org.example.owoonwan.user.domain.UserStatus;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    @Test
    @DisplayName("loginId로 로그인하면 세션 토큰을 발급한다")
    void shouldIssueSessionTokenWhenLoginSucceeds() {
        Instant now = Instant.parse("2026-03-09T00:00:00Z");
        FakeUserRepository userRepository = new FakeUserRepository();
        userRepository.add(new User("u1", "member01", "nick-1", UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));
        FakeSessionRepository sessionRepository = new FakeSessionRepository();
        AuthService authService = new AuthService(
                userRepository,
                sessionRepository,
                new NoopLoginLockRepository(),
                new KstDateTimeProvider(Clock.fixed(now, ZoneOffset.UTC))
        );

        AuthLoginResponse response = authService.login(new AuthLoginRequest("member01"));

        assertEquals("u1", response.uid());
        assertEquals("member01", response.loginId());
        assertEquals("nick-1", response.nicknameId());
    }

    @Test
    @DisplayName("닉네임이 선택되지 않은 사용자는 로그인할 수 없다")
    void shouldRejectWhenNicknameNotSelected() {
        Instant now = Instant.parse("2026-03-09T00:00:00Z");
        FakeUserRepository userRepository = new FakeUserRepository();
        userRepository.add(new User("u1", "member01", null, UserRole.REGULAR, UserStatus.ACTIVE, now, null, null, false, null));
        AuthService authService = new AuthService(
                userRepository,
                new FakeSessionRepository(),
                new NoopLoginLockRepository(),
                new KstDateTimeProvider(Clock.fixed(now, ZoneOffset.UTC))
        );

        assertThrows(BusinessException.class, () -> authService.login(new AuthLoginRequest("member01")));
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

    private static final class FakeSessionRepository implements SessionRepository {
        private final Map<String, Session> sessions = new HashMap<>();

        @Override
        public String create(String userId, String loginId, Instant now, Instant expiresAt) {
            String token = "token-" + sessions.size();
            sessions.put(token, new Session(token, userId, loginId, true, now, now, expiresAt));
            return token;
        }

        @Override
        public Optional<Session> findByToken(String token) {
            return Optional.ofNullable(sessions.get(token));
        }

        @Override
        public void deactivateByToken(String token, Instant now) {
            Session session = sessions.get(token);
            if (session == null) {
                return;
            }
            sessions.put(token, new Session(session.token(), session.userId(), session.loginId(), false, session.createdAt(), now, session.expiresAt()));
        }

        @Override
        public void deactivateAllByUserId(String userId, Instant now) {
            sessions.replaceAll((token, session) -> {
                if (!userId.equals(session.userId())) {
                    return session;
                }
                return new Session(session.token(), session.userId(), session.loginId(), false, session.createdAt(), now, session.expiresAt());
            });
        }

        @Override
        public void touchLastSeen(String token, Instant now) {
            Session session = sessions.get(token);
            if (session == null) {
                return;
            }
            sessions.put(token, new Session(session.token(), session.userId(), session.loginId(), session.active(), session.createdAt(), now, session.expiresAt()));
        }
    }

    private static final class NoopLoginLockRepository implements LoginLockRepository {
        @Override
        public void acquire(String loginId, Instant now, Instant expiresAt) {
        }

        @Override
        public void release(String loginId) {
        }
    }
}

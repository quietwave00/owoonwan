package org.example.owoonwan.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.dto.AuthLoginRequest;
import org.example.owoonwan.auth.dto.AuthLoginResponse;
import org.example.owoonwan.auth.dto.AuthMeResponse;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.session.domain.Session;
import org.example.owoonwan.session.repository.LoginLockRepository;
import org.example.owoonwan.session.repository.SessionRepository;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserStatus;
import org.example.owoonwan.user.repository.UserRepository;
import org.example.owoonwan.user.service.UserAdminService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long LOGIN_LOCK_SECONDS = 5;
    private static final Instant SESSION_MAX_EXPIRES_AT = Instant.parse("9999-12-31T23:59:59Z");

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final LoginLockRepository loginLockRepository;
    private final KstDateTimeProvider dateTimeProvider;

    public AuthLoginResponse login(AuthLoginRequest request) {
        String loginId = UserAdminService.normalizeAndValidateLoginId(request == null ? null : request.loginId());
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.status() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }
        if (user.nicknameId() == null || user.nicknameId().isBlank()) {
            throw new BusinessException(ErrorCode.NICKNAME_NOT_SELECTED);
        }

        Instant now = dateTimeProvider.nowUtc();
        Instant lockExpiresAt = now.plus(LOGIN_LOCK_SECONDS, ChronoUnit.SECONDS);
        loginLockRepository.acquire(loginId, now, lockExpiresAt);
        try {
            sessionRepository.deactivateAllByUserId(user.id(), now);
            Instant sessionExpiresAt = SESSION_MAX_EXPIRES_AT;
            String token = sessionRepository.create(user.id(), user.loginId(), now, sessionExpiresAt);
            userRepository.updateLastLoginAt(user.id(), now);
            return new AuthLoginResponse(
                    token,
                    sessionExpiresAt,
                    user.id(),
                    user.loginId(),
                    user.nicknameId(),
                    user.nicknameDisplay()
            );
        } finally {
            loginLockRepository.release(loginId);
        }
    }

    public void logout(String authorizationHeader) {
        AuthenticatedUser authenticatedUser = authenticate(authorizationHeader);
        logout(authenticatedUser);
    }

    public void logout(AuthenticatedUser authenticatedUser) {
        sessionRepository.deactivateByToken(authenticatedUser.token(), dateTimeProvider.nowUtc());
    }

    public AuthMeResponse me(String authorizationHeader) {
        AuthenticatedUser authenticatedUser = authenticate(authorizationHeader);
        return me(authenticatedUser);
    }

    public AuthMeResponse me(AuthenticatedUser authenticatedUser) {
        Session session = sessionRepository.findByToken(authenticatedUser.token())
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        return new AuthMeResponse(
                authenticatedUser.userId(),
                authenticatedUser.loginId(),
                authenticatedUser.nicknameId(),
                authenticatedUser.nicknameDisplay(),
                authenticatedUser.role(),
                session.expiresAt()
        );
    }

    public AuthenticatedUser authenticate(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        Session session = sessionRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        Instant now = dateTimeProvider.nowUtc();
        if (!session.active()) {
            throw new BusinessException(ErrorCode.SESSION_INACTIVE);
        }
        if (session.expiresAt() == null || !session.expiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.SESSION_EXPIRED);
        }

        User user = userRepository.findById(session.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.status() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }

        sessionRepository.touchLastSeen(token, now);
        return new AuthenticatedUser(
                user.id(),
                user.loginId(),
                user.nicknameId(),
                user.nicknameDisplay(),
                user.role(),
                token
        );
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix) || authorizationHeader.length() <= prefix.length()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return authorizationHeader.substring(prefix.length()).trim();
    }
}

package org.example.owoonwan.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.dto.AuthLoginRequest;
import org.example.owoonwan.auth.dto.AuthLoginResponse;
import org.example.owoonwan.auth.dto.AuthMeResponse;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserStatus;
import org.example.owoonwan.user.repository.UserRepository;
import org.example.owoonwan.user.service.UserAdminService;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;
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
        AuthenticatedUser authenticatedUser = authTokenService.issue(user, now);
        userRepository.updateLastLoginAt(user.id(), now);
        return new AuthLoginResponse(
                authenticatedUser.token(),
                authenticatedUser.expiresAt(),
                user.id(),
                user.loginId(),
                user.nicknameId(),
                user.nicknameDisplay()
        );
    }

    public void logout(String authorizationHeader) {
        AuthenticatedUser authenticatedUser = authenticate(authorizationHeader);
        logout(authenticatedUser);
    }

    public void logout(AuthenticatedUser authenticatedUser) {
    }

    public AuthMeResponse me(String authorizationHeader) {
        AuthenticatedUser authenticatedUser = authenticate(authorizationHeader);
        return me(authenticatedUser);
    }

    public AuthMeResponse me(AuthenticatedUser authenticatedUser) {
        return new AuthMeResponse(
                authenticatedUser.userId(),
                authenticatedUser.loginId(),
                authenticatedUser.nicknameId(),
                authenticatedUser.nicknameDisplay(),
                authenticatedUser.role(),
                authenticatedUser.expiresAt()
        );
    }

    public AuthenticatedUser authenticate(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        Instant now = dateTimeProvider.nowUtc();
        return authTokenService.parse(token, now);
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

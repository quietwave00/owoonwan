package org.example.owoonwan.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.config.AuthTokenProperties;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserRole;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final AuthTokenProperties authTokenProperties;
    private final ObjectMapper objectMapper;

    public AuthenticatedUser issue(User user, Instant now) {
        Instant expiresAt = now.plus(authTokenProperties.getMaxAgeDays(), ChronoUnit.DAYS);
        AuthTokenPayload payload = new AuthTokenPayload(
                user.id(),
                user.loginId(),
                user.nicknameId(),
                user.nicknameDisplay(),
                user.role().name(),
                expiresAt.getEpochSecond()
        );
        return new AuthenticatedUser(
                payload.userId(),
                payload.loginId(),
                payload.nicknameId(),
                payload.nicknameDisplay(),
                user.role(),
                encode(payload),
                expiresAt
        );
    }

    public AuthenticatedUser parse(String token, Instant now) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        String payloadJson = decodeToString(parts[0]);
        byte[] providedSignature = decode(parts[1]);
        byte[] expectedSignature = sign(payloadJson);
        if (!MessageDigest.isEqual(providedSignature, expectedSignature)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        AuthTokenPayload payload;
        try {
            payload = objectMapper.readValue(payloadJson, AuthTokenPayload.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Instant expiresAt = Instant.ofEpochSecond(payload.expiresAtEpochSecond());
        if (!expiresAt.isAfter(now)) {
            throw new BusinessException(ErrorCode.SESSION_EXPIRED);
        }

        return payload.toAuthenticatedUser(token, expiresAt);
    }

    private String encode(AuthTokenPayload payload) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to serialize auth token.");
        }

        return URL_ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8))
                + "."
                + URL_ENCODER.encodeToString(sign(payloadJson));
    }

    private byte[] sign(String payloadJson) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(authTokenProperties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to sign auth token.");
        }
    }

    private String decodeToString(String value) {
        return new String(decode(value), StandardCharsets.UTF_8);
    }

    private byte[] decode(String value) {
        try {
            return URL_DECODER.decode(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private record AuthTokenPayload(
            String userId,
            String loginId,
            String nicknameId,
            String nicknameDisplay,
            String role,
            long expiresAtEpochSecond
    ) {
        private AuthenticatedUser toAuthenticatedUser(String token, Instant expiresAt) {
            return new AuthenticatedUser(
                    userId,
                    loginId,
                    nicknameId,
                    nicknameDisplay,
                    UserRole.valueOf(role),
                    token,
                    expiresAt
            );
        }
    }
}

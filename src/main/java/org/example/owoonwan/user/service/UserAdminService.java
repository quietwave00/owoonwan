package org.example.owoonwan.user.service;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.nickname.repository.NicknameRepository;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserRole;
import org.example.owoonwan.user.dto.AdminCreateUserRequest;
import org.example.owoonwan.user.dto.UserCreateRequest;
import org.example.owoonwan.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]{3,29}$");

    private final UserRepository userRepository;
    private final NicknameRepository nicknameRepository;
    private final KstDateTimeProvider dateTimeProvider;

    public User createUser(AdminCreateUserRequest request) {
        String loginId = normalizeAndValidateLoginId(request == null ? null : request.loginId());
        if (userRepository.existsByLoginId(loginId)) {
            throw new BusinessException(ErrorCode.LOGIN_ID_ALREADY_EXISTS);
        }

        Instant now = dateTimeProvider.nowUtc();
        UserRole role = request.role() == null ? UserRole.REGULAR : request.role();
        String userId = userRepository.create(loginId, role, now);
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public User createRegularUser(UserCreateRequest request) {
        String loginId = normalizeAndValidateLoginId(request == null ? null : request.loginId());
        if (userRepository.existsByLoginId(loginId)) {
            throw new BusinessException(ErrorCode.LOGIN_ID_ALREADY_EXISTS);
        }

        String userId = userRepository.create(loginId, UserRole.REGULAR, dateTimeProvider.nowUtc());
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public User updateRole(String userId, UserRole role) {
        if (role == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "role 값이 필요합니다.");
        }
        return userRepository.updateRole(userId, role);
    }

    public User softDelete(String userId) {
        Instant now = dateTimeProvider.nowUtc();
        User user = userRepository.softDelete(userId, now);
        nicknameRepository.clearAssignment(userId, now);
        return user;
    }

    public User selectNicknameOnce(String userId, String nicknameId) {
        if (nicknameId == null || nicknameId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "nicknameId 값이 필요합니다.");
        }
        Instant now = dateTimeProvider.nowUtc();
        nicknameRepository.assignNicknameToUserFixedOnce(nicknameId, userId, now);
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public List<User> listUsers() {
        return userRepository.findAll();
    }

    private String normalizeAndValidateLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "loginId 값이 필요합니다.");
        }
        String normalized = loginId.trim().toLowerCase();
        if (!LOGIN_ID_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(ErrorCode.LOGIN_ID_INVALID_FORMAT);
        }
        return normalized;
    }
}

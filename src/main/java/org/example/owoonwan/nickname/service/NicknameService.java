package org.example.owoonwan.nickname.service;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.nickname.domain.Nickname;
import org.example.owoonwan.nickname.dto.AdminCreateNicknameRequest;
import org.example.owoonwan.nickname.dto.AdminUpdateNicknameRequest;
import org.example.owoonwan.nickname.repository.NicknameRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NicknameService {

    private final NicknameRepository nicknameRepository;
    private final KstDateTimeProvider dateTimeProvider;

    public Nickname create(AdminCreateNicknameRequest request) {
        if (request == null || request.display() == null || request.display().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "display 값이 필요합니다.");
        }

        Instant now = dateTimeProvider.nowUtc();
        String nicknameId = nicknameRepository.create(request.display().trim(), now);
        return nicknameRepository.findById(nicknameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NICKNAME_NOT_FOUND));
    }

    public Nickname update(String nicknameId, AdminUpdateNicknameRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "요청 본문이 필요합니다.");
        }

        boolean noDisplay = request.display() == null || request.display().isBlank();
        if (noDisplay && request.isActive() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "display 또는 isActive 값이 필요합니다.");
        }

        Instant now = dateTimeProvider.nowUtc();
        return nicknameRepository.update(nicknameId, request.display(), request.isActive(), now);
    }

    public List<Nickname> listForAdmin() {
        return nicknameRepository.findAll();
    }

    public List<Nickname> listActive() {
        return nicknameRepository.findAllActive();
    }
}

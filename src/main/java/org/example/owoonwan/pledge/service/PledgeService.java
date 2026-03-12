package org.example.owoonwan.pledge.service;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.pledge.domain.Pledge;
import org.example.owoonwan.pledge.dto.PledgeResponse;
import org.example.owoonwan.pledge.dto.PledgeUpdateRequest;
import org.example.owoonwan.pledge.repository.PledgeRepository;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserStatus;
import org.example.owoonwan.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PledgeService {

    private static final Collator KOREAN_COLLATOR = Collator.getInstance(Locale.KOREAN);

    private final PledgeRepository pledgeRepository;
    private final UserRepository userRepository;
    private final KstDateTimeProvider dateTimeProvider;

    public PledgeResponse getMyPledge(AuthenticatedUser authenticatedUser) {
        User user = getActiveUser(authenticatedUser.userId());
        return toResponse(resolvePledge(user.id()), user.displayNickname(), true);
    }

    public PledgeResponse upsertMyPledge(AuthenticatedUser authenticatedUser, PledgeUpdateRequest request) {
        User user = getActiveUser(authenticatedUser.userId());
        String text = normalizeText(request);
        Pledge pledge = pledgeRepository.save(user.id(), text, dateTimeProvider.nowUtc());
        return toResponse(pledge, user.displayNickname(), true);
    }

    public List<PledgeResponse> listPledges(String viewerUserId) {
        Map<String, Pledge> pledgesByUserId = pledgeRepository.findAll().stream()
                .collect(Collectors.toMap(Pledge::userId, Function.identity()));
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .toList();
        Map<String, String> nicknameDisplayByUserId = activeUsers.stream()
                .collect(Collectors.toMap(User::id, User::displayNickname));

        return activeUsers.stream()
                .sorted((left, right) -> KOREAN_COLLATOR.compare(
                        nicknameDisplayByUserId.getOrDefault(left.id(), ""),
                        nicknameDisplayByUserId.getOrDefault(right.id(), "")
                ))
                .map(user -> toResponse(
                        pledgesByUserId.getOrDefault(user.id(), emptyPledge(user.id())),
                        nicknameDisplayByUserId.getOrDefault(user.id(), ""),
                        user.id().equals(viewerUserId)
                ))
                .toList();
    }

    public void deletePledge(String userId) {
        getExistingUser(userId);
        pledgeRepository.deleteByUserId(userId);
    }

    private Pledge resolvePledge(String userId) {
        return pledgeRepository.findByUserId(userId).orElseGet(() -> emptyPledge(userId));
    }

    private Pledge emptyPledge(String userId) {
        return new Pledge(userId, "", null, 0);
    }

    private PledgeResponse toResponse(Pledge pledge, String nickname, boolean mine) {
        return PledgeResponse.from(pledge, nickname, mine);
    }

    private User getActiveUser(String userId) {
        User user = getExistingUser(userId);
        if (user.status() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }
        return user;
    }

    private User getExistingUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private String normalizeText(PledgeUpdateRequest request) {
        if (request == null || request.text() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "text 값이 필요합니다.");
        }
        return request.text().trim();
    }
}

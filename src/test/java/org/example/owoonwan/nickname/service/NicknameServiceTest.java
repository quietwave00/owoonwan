package org.example.owoonwan.nickname.service;

import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.time.KstDateTimeProvider;
import org.example.owoonwan.nickname.domain.Nickname;
import org.example.owoonwan.nickname.dto.AdminCreateNicknameRequest;
import org.example.owoonwan.nickname.repository.NicknameRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NicknameServiceTest {

    @Test
    @DisplayName("닉네임 생성 시 공백 display 입력은 거부된다")
    void shouldRejectBlankDisplay() {
        NicknameService service = new NicknameService(
                new NoopNicknameRepository(),
                new KstDateTimeProvider(Clock.fixed(Instant.parse("2026-03-06T00:00:00Z"), ZoneOffset.UTC))
        );

        assertThrows(BusinessException.class, () -> service.create(new AdminCreateNicknameRequest(" ")));
    }

    private static final class NoopNicknameRepository implements NicknameRepository {
        @Override
        public String create(String display, Instant now) {
            return "id";
        }

        @Override
        public Optional<Nickname> findById(String nicknameId) {
            return Optional.of(new Nickname("id", "test", true, null, Instant.now(), Instant.now()));
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

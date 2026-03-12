package org.example.owoonwan.integration;

import com.google.cloud.firestore.Firestore;
import org.example.owoonwan.common.firebase.FirestoreAwait;
import org.example.owoonwan.nickname.domain.Nickname;
import org.example.owoonwan.nickname.repository.NicknameRepository;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserRole;
import org.example.owoonwan.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
//@Disabled("실제 Firestore에 데이터를 쓰는 수동 실행 전용 테스트")
class FirestoreManualSeedIntegrationTest {

    private static final String LOGIN_ID = "admin";
    private static final String NICKNAME_DISPLAY = "관리자";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NicknameRepository nicknameRepository;

    @Autowired
    private Firestore firestore;

    @Test
    @DisplayName("실제 Firestore에 loginId test, 닉네임 테스트계정을 생성한다")
    void shouldSeedTestUserAndNicknameToRealFirestore() {
        cleanupIfExists();

        Instant now = Instant.now();
        String userId = userRepository.create(LOGIN_ID, UserRole.ADMIN, now);
        assertNotNull(userId);

        String nicknameId = nicknameRepository.create(NICKNAME_DISPLAY, now);
        assertNotNull(nicknameId);

        nicknameRepository.assignNicknameToUserFixedOnce(nicknameId, userId, now);

        User savedUser = userRepository.findById(userId).orElseThrow();
        Nickname savedNickname = nicknameRepository.findById(nicknameId).orElseThrow();

        assertEquals(LOGIN_ID, savedUser.loginId());
        assertEquals(nicknameId, savedUser.nicknameId());
        assertEquals(NICKNAME_DISPLAY, savedUser.nicknameDisplay());
        assertEquals(NICKNAME_DISPLAY, savedNickname.display());
        assertEquals(userId, savedNickname.assignedTo());
        assertTrue(savedNickname.active());
    }

    private void cleanupIfExists() {
        userRepository.findByLoginId(LOGIN_ID).ifPresent(existingUser -> {
            if (existingUser.nicknameId() != null && !existingUser.nicknameId().isBlank()) {
                FirestoreAwait.get(firestore.collection("nicknames").document(existingUser.nicknameId()).delete());
            }
            FirestoreAwait.get(firestore.collection("users").document(existingUser.id()).delete());
            FirestoreAwait.get(firestore.collection("loginIds").document(LOGIN_ID).delete());
        });

        FirestoreAwait.get(
                firestore.collection("nicknames")
                        .whereEqualTo("display", NICKNAME_DISPLAY)
                        .get()
        ).getDocuments().forEach(document ->
                FirestoreAwait.get(document.getReference().delete())
        );
    }
}

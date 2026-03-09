package org.example.owoonwan.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import org.example.owoonwan.common.error.BusinessException;
import org.example.owoonwan.common.error.ErrorCode;
import org.example.owoonwan.nickname.domain.Nickname;
import org.example.owoonwan.nickname.repository.NicknameRepository;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserRole;
import org.example.owoonwan.user.domain.UserStatus;
import org.example.owoonwan.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Import(UserNicknameSignupIntegrationTest.InMemoryRepositoryConfig.class)
@TestPropertySource(properties = {
        "app.firebase.enabled=false"
})
class UserNicknameSignupIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("관리자 닉네임 3개 생성 후, 사용자가 닉네임 선택과 loginId 가입을 완료한다")
    void shouldCreateNicknamesAndSignupUserBySelectingNickname() throws Exception {
        String nicknameId1 = createNicknameByAdmin("테스트1");
        String nicknameId2 = createNicknameByAdmin("테스트2");
        String nicknameId3 = createNicknameByAdmin("테스트3");

        assertNotNull(nicknameId1);
        assertNotNull(nicknameId2);
        assertNotNull(nicknameId3);

        mockMvc.perform(get("/nicknames"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));

        String userId = createUser("member01");

        mockMvc.perform(post("/users/" + userId + "/nickname")
                        .header("X-User-Id", userId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"nicknameId\":\"" + nicknameId2 + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uid").value(userId))
                .andExpect(jsonPath("$.data.loginId").value("member01"))
                .andExpect(jsonPath("$.data.nicknameId").value(nicknameId2));

        mockMvc.perform(post("/users/" + userId + "/nickname")
                        .header("X-User-Id", userId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"nicknameId\":\"" + nicknameId1 + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NICKNAME_ALREADY_FIXED"));
    }

    private String createNicknameByAdmin(String display) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/nicknames")
                        .header("X-Role", "ADMIN")
                        .contentType(APPLICATION_JSON)
                        .content("{\"display\":\"" + display + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("nicknameId").asText();
    }

    private String createUser(String loginId) throws Exception {
        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(APPLICATION_JSON)
                        .content("{\"loginId\":\"" + loginId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String userId = json.path("data").path("uid").asText();
        assertEquals(loginId, json.path("data").path("loginId").asText());
        return userId;
    }

    @TestConfiguration
    static class InMemoryRepositoryConfig {

        @Bean
        @Primary
        InMemoryStore inMemoryStore() {
            return new InMemoryStore();
        }

        @Bean
        @Primary
        UserRepository userRepository(InMemoryStore store) {
            return new InMemoryUserRepository(store);
        }

        @Bean
        @Primary
        NicknameRepository nicknameRepository(InMemoryStore store) {
            return new InMemoryNicknameRepository(store);
        }

        @Bean
        Firestore firestore() {
            return mock(Firestore.class);
        }
    }

    static class InMemoryStore {
        private final Map<String, User> users = new LinkedHashMap<>();
        private final Map<String, Nickname> nicknames = new LinkedHashMap<>();
    }

    static class InMemoryUserRepository implements UserRepository {

        private final InMemoryStore store;

        InMemoryUserRepository(InMemoryStore store) {
            this.store = store;
        }

        @Override
        public String create(String loginId, UserRole role, Instant now) {
            String id = UUID.randomUUID().toString();
            User user = new User(id, loginId, null, role, UserStatus.ACTIVE, now, null, null, false, null);
            store.users.put(id, user);
            return id;
        }

        @Override
        public Optional<User> findById(String userId) {
            return Optional.ofNullable(store.users.get(userId));
        }

        @Override
        public boolean existsByLoginId(String loginId) {
            return store.users.values().stream().anyMatch(user -> loginId.equals(user.loginId()));
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(store.users.values());
        }

        @Override
        public User updateRole(String userId, UserRole role) {
            User user = store.users.get(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            User updated = new User(
                    user.id(),
                    user.loginId(),
                    user.nicknameId(),
                    role,
                    user.status(),
                    user.createdAt(),
                    user.deletedAt(),
                    user.lastLoginAt(),
                    user.kakkdugi(),
                    user.pledgeId()
            );
            store.users.put(userId, updated);
            return updated;
        }

        @Override
        public User softDelete(String userId, Instant now) {
            User user = store.users.get(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            User deleted = new User(
                    user.id(),
                    user.loginId(),
                    null,
                    user.role(),
                    UserStatus.DELETED,
                    user.createdAt(),
                    now,
                    user.lastLoginAt(),
                    user.kakkdugi(),
                    user.pledgeId()
            );
            store.users.put(userId, deleted);
            return deleted;
        }
    }

    static class InMemoryNicknameRepository implements NicknameRepository {

        private final InMemoryStore store;

        InMemoryNicknameRepository(InMemoryStore store) {
            this.store = store;
        }

        @Override
        public String create(String display, Instant now) {
            String nicknameId = UUID.randomUUID().toString();
            Nickname nickname = new Nickname(nicknameId, display, true, null, now, now);
            store.nicknames.put(nicknameId, nickname);
            return nicknameId;
        }

        @Override
        public Optional<Nickname> findById(String nicknameId) {
            return Optional.ofNullable(store.nicknames.get(nicknameId));
        }

        @Override
        public List<Nickname> findAll() {
            return new ArrayList<>(store.nicknames.values());
        }

        @Override
        public List<Nickname> findAllActive() {
            return store.nicknames.values().stream()
                    .filter(Nickname::active)
                    .toList();
        }

        @Override
        public Nickname update(String nicknameId, String display, Boolean isActive, Instant now) {
            Nickname current = store.nicknames.get(nicknameId);
            if (current == null) {
                throw new BusinessException(ErrorCode.NICKNAME_NOT_FOUND);
            }
            Nickname updated = new Nickname(
                    current.id(),
                    display == null || display.isBlank() ? current.display() : display.trim(),
                    isActive == null ? current.active() : isActive,
                    current.assignedTo(),
                    current.createdAt(),
                    now
            );
            store.nicknames.put(nicknameId, updated);
            return updated;
        }

        @Override
        public void assignNicknameToUserFixedOnce(String nicknameId, String userId, Instant now) {
            User user = store.users.get(userId);
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            if (user.status() != UserStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
            }
            if (user.nicknameId() != null && !user.nicknameId().isBlank()) {
                throw new BusinessException(ErrorCode.NICKNAME_ALREADY_FIXED);
            }

            Nickname nickname = store.nicknames.get(nicknameId);
            if (nickname == null) {
                throw new BusinessException(ErrorCode.NICKNAME_NOT_FOUND);
            }
            if (!nickname.active()) {
                throw new BusinessException(ErrorCode.NICKNAME_INACTIVE);
            }
            if (nickname.assignedTo() != null && !nickname.assignedTo().isBlank()) {
                throw new BusinessException(ErrorCode.NICKNAME_ALREADY_ASSIGNED);
            }

            User updatedUser = new User(
                    user.id(),
                    user.loginId(),
                    nicknameId,
                    user.role(),
                    user.status(),
                    user.createdAt(),
                    user.deletedAt(),
                    user.lastLoginAt(),
                    user.kakkdugi(),
                    user.pledgeId()
            );
            Nickname updatedNickname = new Nickname(
                    nickname.id(),
                    nickname.display(),
                    nickname.active(),
                    userId,
                    nickname.createdAt(),
                    now
            );
            store.users.put(userId, updatedUser);
            store.nicknames.put(nicknameId, updatedNickname);
        }

        @Override
        public void clearAssignment(String userId, Instant now) {
            store.nicknames.replaceAll((id, nickname) -> {
                if (!userId.equals(nickname.assignedTo())) {
                    return nickname;
                }
                return new Nickname(
                        nickname.id(),
                        nickname.display(),
                        nickname.active(),
                        null,
                        nickname.createdAt(),
                        now
                );
            });
        }
    }
}

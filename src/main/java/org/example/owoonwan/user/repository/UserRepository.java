package org.example.owoonwan.user.repository;

import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserRole;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserRepository {

    String create(String loginId, UserRole role, Instant now);

    Optional<User> findById(String userId);

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    default List<User> findByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Map<String, User> usersById = new LinkedHashMap<>();
        for (String userId : userIds) {
            if (userId == null || userId.isBlank() || usersById.containsKey(userId)) {
                continue;
            }
            findById(userId).ifPresent(user -> usersById.put(userId, user));
        }
        return List.copyOf(usersById.values());
    }

    List<User> findAll();

    User updateRole(String userId, UserRole role);

    User updateKakkdugi(String userId, boolean kakkdugi);

    User softDelete(String userId, Instant now);

    void updateLastLoginAt(String userId, Instant now);
}

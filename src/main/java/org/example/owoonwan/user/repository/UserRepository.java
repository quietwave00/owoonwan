package org.example.owoonwan.user.repository;

import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.domain.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    String create(String loginId, UserRole role, Instant now);

    Optional<User> findById(String userId);

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    List<User> findAll();

    User updateRole(String userId, UserRole role);

    User softDelete(String userId, Instant now);

    void updateLastLoginAt(String userId, Instant now);
}

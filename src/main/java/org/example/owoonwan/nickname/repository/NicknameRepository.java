package org.example.owoonwan.nickname.repository;

import org.example.owoonwan.nickname.domain.Nickname;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NicknameRepository {

    String create(String display, Instant now);

    Optional<Nickname> findById(String nicknameId);

    List<Nickname> findAll();

    List<Nickname> findAllActive();

    Nickname update(String nicknameId, String display, Boolean isActive, Instant now);

    void assignNicknameToUserFixedOnce(String nicknameId, String userId, Instant now);

    void clearAssignment(String userId, Instant now);
}

package org.example.owoonwan.pledge.repository;

import org.example.owoonwan.pledge.domain.Pledge;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PledgeRepository {

    Optional<Pledge> findByUserId(String userId);

    List<Pledge> findAll();

    Pledge save(String userId, String text, Instant now);

    void deleteByUserId(String userId);
}

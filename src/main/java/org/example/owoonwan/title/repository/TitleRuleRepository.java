package org.example.owoonwan.title.repository;

import org.example.owoonwan.title.domain.TitleRules;

import java.util.Optional;

public interface TitleRuleRepository {

    Optional<TitleRules> findRules();
}

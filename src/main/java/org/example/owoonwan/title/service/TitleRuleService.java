package org.example.owoonwan.title.service;

import org.example.owoonwan.title.domain.TitleRules;
import org.example.owoonwan.title.repository.TitleRuleRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TitleRuleService {

    private static final TitleRules DEFAULT_RULES = new TitleRules(3, 12);

    private final ObjectProvider<TitleRuleRepository> titleRuleRepositoryProvider;

    @Autowired
    public TitleRuleService(ObjectProvider<TitleRuleRepository> titleRuleRepositoryProvider) {
        this.titleRuleRepositoryProvider = titleRuleRepositoryProvider;
    }

    public TitleRules getRules() {
        TitleRuleRepository repository = titleRuleRepositoryProvider.getIfAvailable();
        if (repository == null) {
            return DEFAULT_RULES;
        }
        return repository.findRules().orElse(DEFAULT_RULES);
    }
}

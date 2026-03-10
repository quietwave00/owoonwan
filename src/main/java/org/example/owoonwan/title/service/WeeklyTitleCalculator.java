package org.example.owoonwan.title.service;

import org.example.owoonwan.title.domain.TitleRules;
import org.example.owoonwan.title.dto.TitleScopeResponse;
import org.springframework.stereotype.Component;

@Component
public class WeeklyTitleCalculator {

    public TitleScopeResponse calculate(int weeklyCount, TitleRules rules) {
        boolean human = weeklyCount >= rules.weeklyHumanThreshold();
        return new TitleScopeResponse(
                human,
                !human,
                weeklyCount
        );
    }
}

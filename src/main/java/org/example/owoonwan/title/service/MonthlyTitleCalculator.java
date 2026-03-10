package org.example.owoonwan.title.service;

import org.example.owoonwan.title.domain.TitleRules;
import org.example.owoonwan.title.dto.TitleScopeResponse;
import org.springframework.stereotype.Component;

@Component
public class MonthlyTitleCalculator {

    public TitleScopeResponse calculate(int monthlyCount, TitleRules rules) {
        boolean human = monthlyCount >= rules.monthlyHumanThreshold();
        return new TitleScopeResponse(
                human,
                !human,
                monthlyCount
        );
    }
}

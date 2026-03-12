package org.example.owoonwan.title.service;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.title.domain.TitleRules;
import org.example.owoonwan.title.dto.TitleScopeResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TitleBadgeAssembler {

    private final TitleRuleService titleRuleService;
    private final WeeklyTitleCalculator weeklyTitleCalculator;
    private final MonthlyTitleCalculator monthlyTitleCalculator;

    public List<String> buildWeeklyBadges(int weeklyCount, boolean kakkdugi) {
        TitleRules rules = titleRuleService.getRules();
        TitleScopeResponse weekly = weeklyTitleCalculator.calculate(weeklyCount, rules);
        return buildBadges(weekly.human(), weekly.soul(), kakkdugi);
    }

    public List<String> buildMonthlyBadges(int monthlyCount, boolean kakkdugi) {
        TitleRules rules = titleRuleService.getRules();
        TitleScopeResponse monthly = monthlyTitleCalculator.calculate(monthlyCount, rules);
        return buildBadges(monthly.human(), monthly.soul(), kakkdugi);
    }

    private List<String> buildBadges(boolean human, boolean soul, boolean kakkdugi) {
        List<String> badges = new ArrayList<>();
        if (kakkdugi) {
            badges.add("깍두기");
        }
        if (human) {
            badges.add("인간");
        }
        if (soul) {
            badges.add("영혼");
        }
        return List.copyOf(badges);
    }
}

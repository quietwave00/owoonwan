package org.example.owoonwan.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.board.dto.WeeklyBoardResponse;
import org.example.owoonwan.board.service.WeeklyBoardService;
import org.example.owoonwan.stats.dto.MonthlyBoardResponse;
import org.example.owoonwan.stats.service.StatsQueryService;
import org.example.owoonwan.user.domain.UserRole;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private static final AuthenticatedUser ADMIN_VIEWER = new AuthenticatedUser(
            "__admin__",
            "__admin__",
            "__admin__",
            UserRole.ADMIN,
            "__admin__"
    );

    private final WeeklyBoardService weeklyBoardService;
    private final StatsQueryService statsQueryService;

    public WeeklyBoardResponse getWeeklyStats(String date) {
        return weeklyBoardService.getWeeklyBoard(ADMIN_VIEWER, date);
    }

    public MonthlyBoardResponse getMonthlyStats(String month) {
        return statsQueryService.getMonthlyBoard(month);
    }
}

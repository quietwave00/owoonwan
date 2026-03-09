package org.example.owoonwan.stats.controller;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.common.response.ApiResponse;
import org.example.owoonwan.stats.dto.MonthlyBoardResponse;
import org.example.owoonwan.stats.dto.UserMonthlyCalendarResponse;
import org.example.owoonwan.stats.dto.UserSummaryResponse;
import org.example.owoonwan.stats.service.StatsQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.example.owoonwan.auth.AuthAttributes.AUTHENTICATED_USER;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stats")
public class StatsController {

    private final StatsQueryService statsQueryService;

    @GetMapping("/monthly-board")
    public ApiResponse<MonthlyBoardResponse> getMonthlyBoard(
            @RequestParam(value = "month", required = false) String month
    ) {
        return ApiResponse.ok(statsQueryService.getMonthlyBoard(month));
    }

    @GetMapping("/users/{uid}/calendar")
    public ApiResponse<UserMonthlyCalendarResponse> getUserCalendar(
            @RequestAttribute(AUTHENTICATED_USER) AuthenticatedUser authenticatedUser,
            @PathVariable("uid") String userId,
            @RequestParam(value = "month", required = false) String month
    ) {
        return ApiResponse.ok(statsQueryService.getUserCalendar(userId, month, authenticatedUser.userId()));
    }

    @GetMapping("/users/{uid}/summary")
    public ApiResponse<UserSummaryResponse> getUserSummary(
            @PathVariable("uid") String userId
    ) {
        return ApiResponse.ok(statsQueryService.getUserSummary(userId));
    }
}

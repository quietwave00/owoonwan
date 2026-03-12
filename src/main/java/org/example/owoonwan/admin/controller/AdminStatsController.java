package org.example.owoonwan.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.admin.AdminGuard;
import org.example.owoonwan.admin.dto.AdminMonthlyStatsResponse;
import org.example.owoonwan.admin.dto.AdminWeeklyStatsResponse;
import org.example.owoonwan.admin.service.AdminStatsService;
import org.example.owoonwan.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/stats")
public class AdminStatsController {

    private final AdminGuard adminGuard;
    private final AdminStatsService adminStatsService;

    @GetMapping("/weekly")
    public ApiResponse<AdminWeeklyStatsResponse> getWeeklyStats(
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @RequestParam(value = "date", required = false) String date
    ) {
        adminGuard.requireAdmin(roleHeader);
        return ApiResponse.ok(adminStatsService.getWeeklyStats(date));
    }

    @GetMapping("/monthly")
    public ApiResponse<AdminMonthlyStatsResponse> getMonthlyStats(
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @RequestParam(value = "month", required = false) String month
    ) {
        adminGuard.requireAdmin(roleHeader);
        return ApiResponse.ok(adminStatsService.getMonthlyStats(month));
    }
}

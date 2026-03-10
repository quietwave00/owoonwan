package org.example.owoonwan.checkin.controller;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.admin.AdminGuard;
import org.example.owoonwan.checkin.dto.AdminBulkCheckinRequest;
import org.example.owoonwan.checkin.dto.AdminBulkCheckinResponse;
import org.example.owoonwan.checkin.dto.AdminCheckinDateResponse;
import org.example.owoonwan.checkin.dto.CheckinResponse;
import org.example.owoonwan.checkin.service.AdminCheckinQueryService;
import org.example.owoonwan.checkin.service.CheckinService;
import org.example.owoonwan.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/checkins")
public class AdminCheckinController {

    private final AdminGuard adminGuard;
    private final CheckinService checkinService;
    private final AdminCheckinQueryService adminCheckinQueryService;

    @GetMapping
    public ApiResponse<AdminCheckinDateResponse> getCheckinsByDate(
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @RequestParam("date") String date
    ) {
        adminGuard.requireAdmin(roleHeader);
        return ApiResponse.ok(adminCheckinQueryService.getCheckinsByDate(date));
    }

    @PostMapping
    public ApiResponse<AdminBulkCheckinResponse> bulkCheckin(
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @RequestBody AdminBulkCheckinRequest request
    ) {
        adminGuard.requireAdmin(roleHeader);
        List<CheckinResponse> responses = checkinService.bulkCheckinForUsers(request).stream()
                .map(CheckinResponse::from)
                .toList();
        return ApiResponse.ok(new AdminBulkCheckinResponse(request.date(), responses.size(), responses));
    }
}

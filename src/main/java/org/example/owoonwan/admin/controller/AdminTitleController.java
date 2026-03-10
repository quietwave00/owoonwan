package org.example.owoonwan.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.admin.AdminGuard;
import org.example.owoonwan.admin.dto.AdminSpecialTitleResponse;
import org.example.owoonwan.admin.dto.AdminTitleVerificationResponse;
import org.example.owoonwan.admin.service.AdminTitleService;
import org.example.owoonwan.common.response.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminTitleController {

    private final AdminGuard adminGuard;
    private final AdminTitleService adminTitleService;

    @PostMapping("/users/{uid}/special-titles/kakkdugi")
    public ApiResponse<AdminSpecialTitleResponse> assignKakkdugi(
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @PathVariable("uid") String userId
    ) {
        adminGuard.requireAdmin(roleHeader);
        return ApiResponse.ok(adminTitleService.assignKakkdugi(userId));
    }

    @DeleteMapping("/users/{uid}/special-titles/kakkdugi")
    public ApiResponse<AdminSpecialTitleResponse> revokeKakkdugi(
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @PathVariable("uid") String userId
    ) {
        adminGuard.requireAdmin(roleHeader);
        return ApiResponse.ok(adminTitleService.revokeKakkdugi(userId));
    }

    @GetMapping("/titles/verify")
    public ApiResponse<AdminTitleVerificationResponse> verifyTitles(
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @RequestParam(value = "weekKey", required = false) String weekKey,
            @RequestParam(value = "monthKey", required = false) String monthKey
    ) {
        adminGuard.requireAdmin(roleHeader);
        return ApiResponse.ok(adminTitleService.verifyTitles(weekKey, monthKey));
    }
}

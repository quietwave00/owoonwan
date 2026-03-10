package org.example.owoonwan.pledge.controller;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.admin.AdminGuard;
import org.example.owoonwan.common.response.ApiResponse;
import org.example.owoonwan.pledge.service.PledgeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/pledges")
public class AdminPledgeController {

    private final AdminGuard adminGuard;
    private final PledgeService pledgeService;

    @DeleteMapping("/{uid}")
    public ApiResponse<Void> deletePledge(
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @PathVariable("uid") String userId
    ) {
        adminGuard.requireAdmin(roleHeader);
        pledgeService.deletePledge(userId);
        return ApiResponse.ok();
    }
}

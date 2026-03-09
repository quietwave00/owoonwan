package org.example.owoonwan.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.admin.AdminGuard;
import org.example.owoonwan.common.response.ApiResponse;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.dto.AdminCreateUserRequest;
import org.example.owoonwan.user.dto.AdminUpdateUserRoleRequest;
import org.example.owoonwan.user.dto.UserResponse;
import org.example.owoonwan.user.service.UserAdminService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminGuard adminGuard;
    private final UserAdminService userAdminService;

    @PostMapping
    public ApiResponse<UserResponse> createUser(
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @RequestBody AdminCreateUserRequest request
    ) {
        adminGuard.requireAdmin(roleHeader);
        User user = userAdminService.createUser(request);
        return ApiResponse.ok(UserResponse.from(user));
    }

    @DeleteMapping("/{uid}")
    public ApiResponse<UserResponse> deleteUser(
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @PathVariable("uid") String userId
    ) {
        adminGuard.requireAdmin(roleHeader);
        User user = userAdminService.softDelete(userId);
        return ApiResponse.ok(UserResponse.from(user));
    }

    @PatchMapping("/{uid}/role")
    public ApiResponse<UserResponse> updateUserRole(
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @PathVariable("uid") String userId,
            @RequestBody AdminUpdateUserRoleRequest request
    ) {
        adminGuard.requireAdmin(roleHeader);
        User user = userAdminService.updateRole(userId, request.role());
        return ApiResponse.ok(UserResponse.from(user));
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> listUsers(
            @RequestHeader(value = "X-Role", required = false) String roleHeader
    ) {
        adminGuard.requireAdmin(roleHeader);
        List<UserResponse> users = userAdminService.listUsers().stream()
                .map(UserResponse::from)
                .toList();
        return ApiResponse.ok(users);
    }
}

package org.example.owoonwan.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.response.ApiResponse;
import org.example.owoonwan.user.UserGuard;
import org.example.owoonwan.user.domain.User;
import org.example.owoonwan.user.dto.UserCreateRequest;
import org.example.owoonwan.user.dto.UserResponse;
import org.example.owoonwan.user.dto.UserSelectNicknameRequest;
import org.example.owoonwan.user.service.UserAdminService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserGuard userGuard;
    private final UserAdminService userAdminService;

    @PostMapping
    public ApiResponse<UserResponse> createUser(@RequestBody UserCreateRequest request) {
        User user = userAdminService.createRegularUser(request);
        return ApiResponse.ok(UserResponse.from(user));
    }

    @PostMapping("/{uid}/nickname")
    public ApiResponse<UserResponse> selectNickname(
            @PathVariable("uid") String userId,
            @RequestHeader("X-User-Id") String headerUserId,
            @RequestBody UserSelectNicknameRequest request
    ) {
        userGuard.requireSameUser(userId, headerUserId);
        User user = userAdminService.selectNicknameOnce(userId, request.nicknameId());
        return ApiResponse.ok(UserResponse.from(user));
    }
}

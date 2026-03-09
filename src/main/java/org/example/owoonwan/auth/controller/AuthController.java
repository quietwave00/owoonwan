package org.example.owoonwan.auth.controller;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.dto.AuthLoginRequest;
import org.example.owoonwan.auth.dto.AuthLoginResponse;
import org.example.owoonwan.auth.dto.AuthMeResponse;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.auth.service.AuthService;
import org.example.owoonwan.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.example.owoonwan.auth.AuthAttributes.AUTHENTICATED_USER;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthLoginResponse> login(@RequestBody AuthLoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestAttribute(AUTHENTICATED_USER) AuthenticatedUser authenticatedUser) {
        authService.logout(authenticatedUser);
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<AuthMeResponse> me(@RequestAttribute(AUTHENTICATED_USER) AuthenticatedUser authenticatedUser) {
        return ApiResponse.ok(authService.me(authenticatedUser));
    }
}

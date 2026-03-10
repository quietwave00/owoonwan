package org.example.owoonwan.title.controller;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.common.response.ApiResponse;
import org.example.owoonwan.title.dto.TitleResponse;
import org.example.owoonwan.title.service.TitleQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.example.owoonwan.auth.AuthAttributes.AUTHENTICATED_USER;

@RestController
@RequiredArgsConstructor
@RequestMapping("/titles")
public class TitleController {

    private final TitleQueryService titleQueryService;

    @GetMapping("/me/current-week")
    public ApiResponse<TitleResponse> getMyCurrentWeek(
            @RequestAttribute(AUTHENTICATED_USER) AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.ok(titleQueryService.getMyCurrentWeek(authenticatedUser));
    }

    @GetMapping("/me/current-month")
    public ApiResponse<TitleResponse> getMyCurrentMonth(
            @RequestAttribute(AUTHENTICATED_USER) AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.ok(titleQueryService.getMyCurrentMonth(authenticatedUser));
    }

    @GetMapping("/users/{uid}")
    public ApiResponse<TitleResponse> getUserTitles(
            @PathVariable("uid") String userId,
            @RequestParam(value = "weekKey", required = false) String weekKey,
            @RequestParam(value = "monthKey", required = false) String monthKey
    ) {
        return ApiResponse.ok(titleQueryService.getUserTitles(userId, weekKey, monthKey));
    }
}

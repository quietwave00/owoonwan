package org.example.owoonwan.checkin.controller;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.checkin.dto.CheckinPeriodResponse;
import org.example.owoonwan.checkin.dto.CheckinResponse;
import org.example.owoonwan.checkin.dto.WeeklyBoardResponse;
import org.example.owoonwan.checkin.service.CheckinService;
import org.example.owoonwan.common.response.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.example.owoonwan.auth.AuthAttributes.AUTHENTICATED_USER;

@RestController
@RequiredArgsConstructor
@RequestMapping("/checkins")
public class CheckinController {

    private final CheckinService checkinService;

    @PostMapping("/today")
    public ApiResponse<CheckinResponse> checkinToday(
            @RequestAttribute(AUTHENTICATED_USER) AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.ok(CheckinResponse.from(checkinService.checkinToday(authenticatedUser)));
    }

    @DeleteMapping("/today")
    public ApiResponse<CheckinResponse> cancelToday(
            @RequestAttribute(AUTHENTICATED_USER) AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.ok(CheckinResponse.from(checkinService.cancelToday(authenticatedUser)));
    }

    @GetMapping("/me/week")
    public ApiResponse<CheckinPeriodResponse> getMyWeek(
            @RequestAttribute(AUTHENTICATED_USER) AuthenticatedUser authenticatedUser,
            @RequestParam(value = "date", required = false) String date
    ) {
        return ApiResponse.ok(checkinService.getMyWeek(authenticatedUser, date));
    }

    @GetMapping("/me/month")
    public ApiResponse<CheckinPeriodResponse> getMyMonth(
            @RequestAttribute(AUTHENTICATED_USER) AuthenticatedUser authenticatedUser,
            @RequestParam(value = "month", required = false) String month
    ) {
        return ApiResponse.ok(checkinService.getMyMonth(authenticatedUser, month));
    }

    @GetMapping("/users/{uid}/month")
    public ApiResponse<CheckinPeriodResponse> getUserMonth(
            @RequestAttribute(AUTHENTICATED_USER) AuthenticatedUser authenticatedUser,
            @PathVariable("uid") String userId,
            @RequestParam(value = "month", required = false) String month
    ) {
        return ApiResponse.ok(checkinService.getUserMonth(userId, month, authenticatedUser.userId()));
    }

    @GetMapping("/week-board")
    public ApiResponse<WeeklyBoardResponse> getWeekBoard(
            @RequestAttribute(AUTHENTICATED_USER) AuthenticatedUser authenticatedUser,
            @RequestParam(value = "date", required = false) String date
    ) {
        return ApiResponse.ok(checkinService.getWeekBoard(authenticatedUser, date));
    }
}

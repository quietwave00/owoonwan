package org.example.owoonwan.board.controller;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.board.dto.WeeklyBoardResponse;
import org.example.owoonwan.board.service.WeeklyBoardService;
import org.example.owoonwan.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.example.owoonwan.auth.AuthAttributes.AUTHENTICATED_USER;

@RestController
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {

    private final WeeklyBoardService weeklyBoardService;

    @GetMapping("/weekly")
    public ApiResponse<WeeklyBoardResponse> getWeeklyBoard(
            @RequestAttribute(AUTHENTICATED_USER) AuthenticatedUser authenticatedUser,
            @RequestParam(value = "date", required = false) String date
    ) {
        return ApiResponse.ok(weeklyBoardService.getWeeklyBoard(authenticatedUser, date));
    }
}

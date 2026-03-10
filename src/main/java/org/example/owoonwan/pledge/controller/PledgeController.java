package org.example.owoonwan.pledge.controller;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.auth.dto.AuthenticatedUser;
import org.example.owoonwan.common.response.ApiResponse;
import org.example.owoonwan.pledge.dto.PledgeResponse;
import org.example.owoonwan.pledge.dto.PledgeUpdateRequest;
import org.example.owoonwan.pledge.service.PledgeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.example.owoonwan.auth.AuthAttributes.AUTHENTICATED_USER;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pledges")
public class PledgeController {

    private final PledgeService pledgeService;

    @GetMapping("/me")
    public ApiResponse<PledgeResponse> getMyPledge(
            @RequestAttribute(AUTHENTICATED_USER) AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.ok(pledgeService.getMyPledge(authenticatedUser));
    }

    @PutMapping("/me")
    public ApiResponse<PledgeResponse> updateMyPledge(
            @RequestAttribute(AUTHENTICATED_USER) AuthenticatedUser authenticatedUser,
            @RequestBody PledgeUpdateRequest request
    ) {
        return ApiResponse.ok(pledgeService.upsertMyPledge(authenticatedUser, request));
    }

    @GetMapping
    public ApiResponse<List<PledgeResponse>> listPledges(
            @RequestAttribute(AUTHENTICATED_USER) AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.ok(pledgeService.listPledges(authenticatedUser.userId()));
    }
}

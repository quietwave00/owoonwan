package org.example.owoonwan.nickname.controller;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.common.response.ApiResponse;
import org.example.owoonwan.nickname.dto.NicknameResponse;
import org.example.owoonwan.nickname.service.NicknameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/nicknames")
public class NicknameController {

    private final NicknameService nicknameService;

    @GetMapping
    public ApiResponse<List<NicknameResponse>> listActiveNicknames() {
        List<NicknameResponse> responses = nicknameService.listActive().stream()
                .map(NicknameResponse::from)
                .toList();
        return ApiResponse.ok(responses);
    }
}

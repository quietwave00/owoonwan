package org.example.owoonwan.nickname.controller;

import lombok.RequiredArgsConstructor;
import org.example.owoonwan.admin.AdminGuard;
import org.example.owoonwan.common.response.ApiResponse;
import org.example.owoonwan.nickname.domain.Nickname;
import org.example.owoonwan.nickname.dto.AdminCreateNicknameRequest;
import org.example.owoonwan.nickname.dto.AdminUpdateNicknameRequest;
import org.example.owoonwan.nickname.dto.NicknameResponse;
import org.example.owoonwan.nickname.service.NicknameService;
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
@RequestMapping("/admin/nicknames")
public class AdminNicknameController {

    private final AdminGuard adminGuard;
    private final NicknameService nicknameService;

    @PostMapping
    public ApiResponse<NicknameResponse> createNickname(
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @RequestBody AdminCreateNicknameRequest request
    ) {
        adminGuard.requireAdmin(roleHeader);
        Nickname nickname = nicknameService.create(request);
        return ApiResponse.ok(NicknameResponse.from(nickname));
    }

    @PatchMapping("/{nicknameId}")
    public ApiResponse<NicknameResponse> updateNickname(
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @PathVariable String nicknameId,
            @RequestBody AdminUpdateNicknameRequest request
    ) {
        adminGuard.requireAdmin(roleHeader);
        Nickname nickname = nicknameService.update(nicknameId, request);
        return ApiResponse.ok(NicknameResponse.from(nickname));
    }

    @GetMapping
    public ApiResponse<List<NicknameResponse>> listNicknames(
            @RequestHeader(value = "X-Role", required = false) String roleHeader
    ) {
        adminGuard.requireAdmin(roleHeader);
        List<NicknameResponse> responses = nicknameService.listForAdmin().stream()
                .map(NicknameResponse::from)
                .toList();
        return ApiResponse.ok(responses);
    }
}

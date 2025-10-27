package com.space.munova.member.controller;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.member.dto.GetMemberResponse;
import com.space.munova.member.dto.UpdateMemberRequest;
import com.space.munova.member.dto.UpdateMemberResponse;
import com.space.munova.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 유저 정보 조회
     */
    @GetMapping("/{memberId}")
    public ResponseApi<GetMemberResponse> getMember(@PathVariable Long memberId) {
        GetMemberResponse member = memberService.getMember(memberId);
        return ResponseApi.ok(member);
    }

    /**
     * 유저 정보 변경
     */
    @PatchMapping("/{memberId}")
    public ResponseApi<UpdateMemberResponse> updateMember(
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRequest updateMemberRequest
    ) {
        UpdateMemberResponse updateMemberResponse = memberService.updateMember(memberId, updateMemberRequest);
        return ResponseApi.ok(updateMemberResponse);
    }

}

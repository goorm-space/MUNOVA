package com.space.munova.auth.dto;

import com.space.munova.member.dto.MemberRole;

public record SignInResponse(
        Long memberId,
        String username,
        String accessToken,
        String refreshToken,
        MemberRole role
) {

    public static SignInResponse of(
            Long memberId, String username, String accessToken, String refreshToken, MemberRole role
    ) {
        return new SignInResponse(memberId, username, accessToken, refreshToken, role);
    }
}

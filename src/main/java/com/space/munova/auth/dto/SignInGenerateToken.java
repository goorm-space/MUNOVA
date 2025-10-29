package com.space.munova.auth.dto;

import com.space.munova.member.dto.MemberRole;

public record SignInGenerateToken(
        Long memberId,
        String username,
        String accessToken,
        String refreshToken,
        MemberRole role
) {
    public static SignInGenerateToken of(
            Long memberId, String username, String accessToken, String refreshToken, MemberRole role
    ) {
        return new SignInGenerateToken(memberId, username, accessToken, refreshToken, role);
    }
}

package com.space.munova.auth.dto;

public record SignInGenerateToken(
        Long memberId,
        String accessToken,
        String refreshToken
) {
    public static SignInGenerateToken of(Long memberId, String accessToken, String refreshToken) {
        return new SignInGenerateToken(memberId, accessToken, refreshToken);
    }
}

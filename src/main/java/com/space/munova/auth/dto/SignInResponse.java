package com.space.munova.auth.dto;

public record SignInResponse(
        Long memberId,
        String accessToken,
        String refreshToken
) {

    public static SignInResponse of(Long memberId, String accessToken, String refreshToken) {
        return new SignInResponse(memberId, accessToken, refreshToken);
    }
}

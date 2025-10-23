package com.space.munova.auth.dto;

public record SignInResponse(
        Long memberId,
        String accessToken
) {

    public static SignInResponse of(Long memberId, String accessToken) {
        return new SignInResponse(memberId, accessToken);
    }
}

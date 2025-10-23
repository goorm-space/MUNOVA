package com.space.munova.auth.dto;

public record GenerateTokens(
        String accessToken,
        String refreshToken
) {
    public static GenerateTokens of(String accessToken, String refreshToken) {
        return new GenerateTokens(accessToken, refreshToken);
    }
}

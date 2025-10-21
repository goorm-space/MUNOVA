package com.space.munova.auth.dto;

public record SignupResponse(
        Long userId,
        String username
) {
    public static SignupResponse of(Long userId, String username) {
        return new SignupResponse(userId, username);
    }
}

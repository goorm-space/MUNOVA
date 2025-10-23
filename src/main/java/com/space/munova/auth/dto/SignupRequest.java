package com.space.munova.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank(message = "사용자명 필수")
        String username,

        @NotBlank(message = "비밀번호 필수")
        String password
) {
    public static SignupRequest of(String username, String password) {
        return new SignupRequest(username, password);
    }
}

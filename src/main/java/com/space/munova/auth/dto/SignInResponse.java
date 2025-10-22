package com.space.munova.auth.dto;

public record SignInResponse(String accessToken) {

    public static SignInResponse of(String accessToken) {
        return new SignInResponse(accessToken);
    }
}

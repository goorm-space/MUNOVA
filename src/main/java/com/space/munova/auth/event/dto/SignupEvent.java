package com.space.munova.auth.event.dto;

import com.space.munova.auth.dto.SignupRequest;

public record SignupEvent(
        String username,
        String password,
        String address
) {
    public static SignupEvent from(SignupRequest signupRequest) {
        return new SignupEvent(
                signupRequest.username(),
                signupRequest.password(),
                signupRequest.address()
        );
    }
}

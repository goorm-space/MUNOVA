package com.space.munova.auth.dto;

public record TokenReissueResponse(String accessToken) {

    public static TokenReissueResponse of(String accessToken) {
        return new TokenReissueResponse(accessToken);
    }
}

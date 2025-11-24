package com.space.munova.velvetQ.dto;

public record ExternalQueueResponse(boolean required, String redirectUrl) {

    public static ExternalQueueResponse of(boolean required, String redirectUrl) {
        return new ExternalQueueResponse(required, redirectUrl);
    }
}

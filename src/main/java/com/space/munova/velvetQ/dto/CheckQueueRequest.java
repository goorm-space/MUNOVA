package com.space.munova.velvetQ.dto;

public record CheckQueueRequest(VelvetQDomainType domain, String resourceId) {

    public static CheckQueueRequest of(VelvetQDomainType domain, String resourceId) {
        return new CheckQueueRequest(domain, resourceId);
    }
}

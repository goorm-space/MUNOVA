package com.space.munova.notification.dto;

public record SearchNotificationUnreadResponse(Long count) {

    public static SearchNotificationUnreadResponse of(Long count) {
        return new SearchNotificationUnreadResponse(count);
    }
}

package com.space.munova.notification.dto;

import com.space.munova.notification.common.NotificationMessage;

import java.time.LocalDateTime;

public record NotificationSseResponse(
        Long notificationId,
        String title,
        String content,
        String redirectUrl,
        String notificationType,
        LocalDateTime createdAt
) {
    public static NotificationSseResponse from(NotificationPayload payload) {
        NotificationMessage notificationMessage = payload.notificationData();

        return new NotificationSseResponse(
                0L,
                notificationMessage.getTitle(),
                payload.message(),
                notificationMessage.getRedirectUrl(),
                notificationMessage.getNotificationType().name(),
                LocalDateTime.now()
        );
    }

    public static NotificationSseResponse from(Long notificationId, NotificationPayload payload) {
        NotificationMessage notificationMessage = payload.notificationData();

        return new NotificationSseResponse(
                notificationId,
                notificationMessage.getTitle(),
                payload.message(),
                notificationMessage.getRedirectUrl(),
                notificationMessage.getNotificationType().name(),
                LocalDateTime.now()
        );
    }
}
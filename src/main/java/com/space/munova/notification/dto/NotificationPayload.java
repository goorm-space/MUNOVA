package com.space.munova.notification.dto;

import com.space.munova.notification.common.NotificationMessage;

public record NotificationPayload(
        Object emitterId,
        Long memberId,
        String message,
        NotificationType type,
        NotificationMessage notificationData
) {
    public static NotificationPayload of(
            Object emitterId,
            Long memberId,
            NotificationType type,
            NotificationMessage notificationData,
            Object... messageFormatArgs
    ) {
        String message = notificationData.format(messageFormatArgs);
        return new NotificationPayload(emitterId, memberId, message, type, notificationData);
    }
}

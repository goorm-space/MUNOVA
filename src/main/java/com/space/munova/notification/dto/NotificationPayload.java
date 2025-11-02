package com.space.munova.notification.dto;

import com.space.munova.notification.common.NotificationMessage;

public record NotificationPayload(
        Object emitterId,
        Long memberId,
        NotificationType type,
        NotificationMessage notificationData
) {
}

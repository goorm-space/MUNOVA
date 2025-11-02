package com.space.munova.notification.common;

import com.space.munova.notification.dto.NotificationType;

public interface NotificationMessage {
    String getTitle();

    String getMessage();

    String getRedirectUrl();

    NotificationType getNotificationType();

    default String format(Object... args) {
        return String.format(getMessage(), args);
    }
}

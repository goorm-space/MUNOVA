package com.space.munova.notification.dto;

import com.space.munova.notification.common.NotificationMessage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ConnectNotification implements NotificationMessage {
    CLIENT_CONNECT("서버연결", "서버 SSE 연결 성공", "");

    private final String title;
    private final String message;
    private final String redirectUrl;

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getRedirectUrl() {
        return redirectUrl;
    }

    @Override
    public NotificationType getNotificationType() {
        return NotificationType.SYSTEM;
    }
}

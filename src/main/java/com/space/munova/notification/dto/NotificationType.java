package com.space.munova.notification.dto;

import lombok.Getter;

@Getter
public enum NotificationType {
    COUPON("쿠폰", true),
    PAYMENT("결제", true),
    SYSTEM("시스템", false),
    CHAT("채팅", false);

    private final String description;
    private final boolean shouldSave;

    NotificationType(String description, boolean shouldSave) {
        this.description = description;
        this.shouldSave = shouldSave;
    }
}

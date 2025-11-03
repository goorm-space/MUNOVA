package com.space.munova.payment.dto;

import com.space.munova.notification.common.NotificationMessage;
import com.space.munova.notification.dto.NotificationType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PaymentNotification implements NotificationMessage {
    PAYMENT_CONFIRM("결제완료안내", "[결제완료] 주문번호: %s\n 가격: %s\n 결제가 완료되었습니다.", "/mypage?activeTab=orders");

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
        return NotificationType.PAYMENT;
    }
}

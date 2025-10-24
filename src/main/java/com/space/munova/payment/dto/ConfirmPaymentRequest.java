package com.space.munova.payment.dto;

public record ConfirmPaymentRequest(
        String paymentKey,
        String orderId,
        Long amount
) {
}

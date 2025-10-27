package com.space.munova.payment.dto;

public record CancelPaymentRequest(
        String cancelReason,
        Long cancelAmount
) {
}

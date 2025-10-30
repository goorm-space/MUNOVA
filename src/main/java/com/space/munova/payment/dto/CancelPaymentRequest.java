package com.space.munova.payment.dto;

public record CancelPaymentRequest(
        CancelReason cancelReason,
        Long cancelAmount
) {
}

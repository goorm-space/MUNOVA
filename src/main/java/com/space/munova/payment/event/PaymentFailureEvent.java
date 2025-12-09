package com.space.munova.payment.event;

public record PaymentFailureEvent(
        String paymentKey,
        String orderNum,
        Long amount
) {
}


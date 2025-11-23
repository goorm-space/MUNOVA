package com.space.munova.payment.event;

public record PaymentCompensationEvent(
        String paymentKey,
        String orderNum,
        Long amount

) {
}

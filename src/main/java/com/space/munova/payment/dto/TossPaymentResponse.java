package com.space.munova.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.space.munova.payment.entity.PaymentMethod;
import com.space.munova.payment.entity.PaymentStatus;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentResponse (
        String paymentKey,
        String orderId,
        PaymentStatus status,
        PaymentMethod method,
        Long totalAmount,
        ZonedDateTime requestedAt,
        ZonedDateTime approvedAt,
        ReceiptInfo receipt,
        String lastTransactionKey,
        CancelDto cancels
){
}

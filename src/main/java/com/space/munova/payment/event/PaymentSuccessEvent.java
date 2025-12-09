package com.space.munova.payment.event;

import com.space.munova.payment.dto.CancelDto;
import com.space.munova.payment.dto.ReceiptInfo;
import com.space.munova.payment.dto.TossPaymentResponse;
import com.space.munova.payment.entity.PaymentMethod;
import com.space.munova.payment.entity.PaymentStatus;

import java.time.ZonedDateTime;
import java.util.List;

public record PaymentSuccessEvent(
        Long memberId,
        String paymentKey,
        String orderNum,
        PaymentStatus status,
        PaymentMethod method,
        Long totalAmount,
        ZonedDateTime requestedAt,
        ZonedDateTime approvedAt,
        ReceiptInfo receipt,
        String lastTransactionKey,
        List<CancelDto> cancels

) {
    public static PaymentSuccessEvent from(Long memberId, TossPaymentResponse response) {
        return new PaymentSuccessEvent(
                memberId,
                response.paymentKey(),
                response.orderId(),
                response.status(),
                response.method(),
                response.totalAmount(),
                response.requestedAt(),
                response.approvedAt(),
                response.receipt(),
                response.lastTransactionKey(),
                response.cancels()
        );
    }
}

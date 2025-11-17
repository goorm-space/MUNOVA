package com.space.munova.payment.service;

import com.space.munova.order.dto.CancelOrderItemRequest;
import com.space.munova.payment.dto.ConfirmPaymentRequest;
import com.space.munova.payment.entity.Payment;

public interface PaymentService {
    void confirmPaymentAndSavePayment(ConfirmPaymentRequest requestBody, Long memberId);
    Payment getPaymentByOrderId(Long orderId);
    void cancelPaymentAndSaveRefund(Long orderItemId, Long orderId, CancelOrderItemRequest request);
}
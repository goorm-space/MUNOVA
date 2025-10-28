package com.space.munova.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.space.munova.order.dto.CancelOrderItemRequest;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.payment.dto.ConfirmPaymentRequest;
import com.space.munova.payment.entity.Payment;

public interface PaymentService {
    void confirmPaymentAndSavePayment(ConfirmPaymentRequest requestBody);
    Payment getPaymentByOrderId(Long orderId);
    void cancelPaymentAndSaveRefund(OrderItem orderItem, Long orderId, CancelOrderItemRequest request);
}
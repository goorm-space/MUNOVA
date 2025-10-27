package com.space.munova.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.space.munova.payment.entity.Payment;

public interface PaymentService {
    void savePaymentInfo(String responseBody) throws JsonProcessingException;
    Payment getPaymentInfo(Long orderId);
}

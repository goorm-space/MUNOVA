package com.space.munova.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface PaymentService {
    void savePaymentInfo(String responseBody) throws JsonProcessingException;
}

package com.space.munova.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.order.entity.Order;
import com.space.munova.payment.dto.TossPaymentResponse;
import com.space.munova.payment.entity.Payment;
import com.space.munova.payment.entity.PaymentStatus;
import com.space.munova.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    @Override
    public void savePaymentInfo(String responseBody) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        TossPaymentResponse paymentResponse = objectMapper.readValue(responseBody, TossPaymentResponse.class);

        if (paymentResponse.status().equals(PaymentStatus.DONE)) {
            // Todo: 주문 id와 결제 금액 일치하는지 최종 검증
            // Todo: OrderEntity 저장
            Order order = Order.builder().build();

            Payment payment = Payment.builder()
                    .order(order)
                    .tossPaymentKey(paymentResponse.paymentKey())
                    .status(paymentResponse.status())
                    .totalAmount(paymentResponse.totalAmount())
                    .requestedAt(paymentResponse.requestedAt())
                    .approvedAt(paymentResponse.approvedAt())
                    .receipt(paymentResponse.receipt().url())
                    .lastTransactionKey(paymentResponse.lastTransactionKey())
                    .paymentObject(responseBody)
                    .build();

            paymentRepository.save(payment);
        }
    }
}

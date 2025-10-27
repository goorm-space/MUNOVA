package com.space.munova.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.Order;
import com.space.munova.order.repository.OrderRepository;
import com.space.munova.payment.dto.TossPaymentResponse;
import com.space.munova.payment.entity.Payment;
import com.space.munova.payment.entity.PaymentStatus;
import com.space.munova.payment.exception.PaymentException;
import com.space.munova.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    @Override
    public void savePaymentInfo(String responseBody) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        TossPaymentResponse paymentResponse = objectMapper.readValue(responseBody, TossPaymentResponse.class);

        if (paymentResponse.status().equals(PaymentStatus.DONE)) {
            Order order = orderRepository.findByOrderNum(paymentResponse.orderId());

            if (!paymentResponse.totalAmount().equals(order.getTotalPrice())) {
                throw PaymentException.amountMismatchException(
                        "payments: " + paymentResponse.totalAmount() + "server: " +  order.getTotalPrice()
                );
            }

            order.updateStatus(OrderStatus.PAID);

            Payment payment = Payment.builder()
                    .order(order)
                    .tossPaymentKey(paymentResponse.paymentKey())
                    .status(paymentResponse.status())
                    .method(paymentResponse.method())
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

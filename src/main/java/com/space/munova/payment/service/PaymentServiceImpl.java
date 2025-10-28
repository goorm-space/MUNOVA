package com.space.munova.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.space.munova.order.dto.CancelOrderItemRequest;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.Order;
import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.repository.OrderRepository;
import com.space.munova.payment.client.TossApiClient;
import com.space.munova.payment.dto.CancelDto;
import com.space.munova.payment.dto.CancelPaymentRequest;
import com.space.munova.payment.dto.ConfirmPaymentRequest;
import com.space.munova.payment.dto.TossPaymentResponse;
import com.space.munova.payment.entity.Payment;
import com.space.munova.payment.entity.PaymentStatus;
import com.space.munova.payment.entity.Refund;
import com.space.munova.payment.exception.PaymentException;
import com.space.munova.payment.repository.PaymentRepository;
import com.space.munova.payment.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final TossApiClient tossApiClient;

    @Transactional
    @Override
    public void confirmPaymentAndSavePayment(ConfirmPaymentRequest request) {
        String tossResponse = tossApiClient.sendConfirmRequest(request);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            TossPaymentResponse response = objectMapper.registerModule(new JavaTimeModule()).readValue(tossResponse, TossPaymentResponse.class);

            if (response.status().equals(PaymentStatus.DONE)) {
                Order order = orderRepository.findByOrderNum(response.orderId());

                if (!response.totalAmount().equals(order.getTotalPrice())) {
                    throw PaymentException.amountMismatchException(
                            String.format("payments: %d, server: %d", response.totalAmount(), order.getTotalPrice())
                    );
                }

                order.updateStatus(OrderStatus.PAID);

                Payment payment = Payment.builder()
                        .order(order)
                        .tossPaymentKey(response.paymentKey())
                        .status(response.status())
                        .method(response.method())
                        .totalAmount(response.totalAmount())
                        .requestedAt(response.requestedAt())
                        .approvedAt(response.approvedAt())
                        .receipt(response.receipt().url())
                        .lastTransactionKey(response.lastTransactionKey())
                        .paymentObject(tossResponse)
                        .build();

                paymentRepository.save(payment);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 변환 오류 발생", e);
        }

    }

    @Override
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findPaymentByOrderId(orderId)
                .orElseThrow(PaymentException::orderMismatchException);
    }

    @Transactional
    @Override
    public void cancelPaymentAndSaveRefund(OrderItem orderItem, Long orderId, CancelOrderItemRequest request) {
        Payment payment = getPaymentByOrderId(orderId);

        CancelPaymentRequest paymentRequest = new CancelPaymentRequest(request.cancelReason(), request.cancelAmount());

        String tossResponse = tossApiClient.sendCancelRequest(payment.getTossPaymentKey(), paymentRequest);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            TossPaymentResponse response = objectMapper.registerModule(new JavaTimeModule()).readValue(tossResponse, TossPaymentResponse.class);

            for (CancelDto cancel : response.cancels()) {
                String transactionKey = cancel.transactionKey();

                if (refundRepository.findByTransactionKey(transactionKey).isPresent()) {
                    continue;
                }

                if (cancel.cancelStatus().equals("DONE")) {
                    payment.updatePaymentInfo(response, tossResponse);

                    Refund refund = Refund.builder()
                            .payment(payment)
                            .orderItem(orderItem)
                            .transactionKey(cancel.transactionKey())
                            .cancelReason(cancel.cancelReason())
                            .cancelAmount(cancel.cancelAmount())
                            .cancelStatus(cancel.cancelStatus())
                            .canceledAt(cancel.canceledAt())
                            .build();

                    refundRepository.save(refund);
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 변환 오류 발생", e);
        }
    }
}

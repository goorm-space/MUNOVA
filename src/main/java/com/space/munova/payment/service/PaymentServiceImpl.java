package com.space.munova.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.space.munova.coupon.service.CouponService;
import com.space.munova.notification.dto.NotificationPayload;
import com.space.munova.notification.dto.NotificationType;
import com.space.munova.notification.service.NotificationService;
import com.space.munova.order.dto.CancelOrderItemRequest;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.Order;
import com.space.munova.order.exception.OrderException;
import com.space.munova.order.repository.OrderRepository;
import com.space.munova.payment.client.TossApiClient;
import com.space.munova.payment.dto.CancelDto;
import com.space.munova.payment.dto.CancelPaymentRequest;
import com.space.munova.payment.dto.ConfirmPaymentRequest;
import com.space.munova.payment.dto.TossPaymentResponse;
import com.space.munova.payment.entity.Payment;
import com.space.munova.payment.entity.PaymentStatus;
import com.space.munova.payment.entity.Refund;
import com.space.munova.payment.event.PaymentCompensationEvent;
import com.space.munova.payment.exception.PaymentException;
import com.space.munova.payment.repository.PaymentRepository;
import com.space.munova.payment.repository.RefundRepository;
import com.space.munova.product.application.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.space.munova.payment.dto.PaymentNotification.PAYMENT_CONFIRM;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final TossApiClient tossApiClient;
    private final CartService cartService;
    private final NotificationService notificationService;
    private final CouponService couponService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public void confirmPaymentAndSavePayment(ConfirmPaymentRequest request, Long memberId) {
        Order order = orderRepository.findByOrderNum(request.orderId())
                .orElseThrow(OrderException::notFoundException);

        if (!request.amount().equals(order.getTotalPrice())) {
            throw PaymentException.amountMismatchException(
                    String.format("요청금액: %d, 실제금액: %d", request.amount(), order.getTotalPrice())
            );
        }

        String tossResponse = tossApiClient.sendConfirmRequest(request);
        eventPublisher.publishEvent(new PaymentCompensationEvent(request.paymentKey(), request.orderId(), request.amount()));

        TossPaymentResponse response;
        try {
            response = objectMapper.readValue(tossResponse, TossPaymentResponse.class);

        } catch (JsonProcessingException e) {
            throw PaymentException.jsonParsingException();
        }

        if (!PaymentStatus.DONE.equals(response.status())) {
            throw PaymentException.paymentStatusException(
                    String.format("현재 결제 상태: %s", response.status())
            );
        }


        if (!response.totalAmount().equals(order.getTotalPrice())) {
            throw PaymentException.amountMismatchException(
                    String.format("결제금액: %d, 실제금액: %d", response.totalAmount(), order.getTotalPrice())
            );
        }

        order.updateStatus(OrderStatus.PAID);

        if (order.getCouponId() != null) {
            couponService.useCoupon(order.getCouponId());
        }

        Payment payment = Payment.builder()
                .orderId(order.getId())
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

        // 장바구니 삭제
        List<Long> productDetailIds = order.getOrderItems().stream()
                .map(orderItem -> orderItem.getProductDetail().getId())
                .toList();
        cartService.deleteByProductDetailIdsAndMemberId(productDetailIds, memberId);

        // 알림 발송
        sendPaymentNotification(memberId, order.getOrderNum(), payment.getTotalAmount());
    }

    @Override
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findPaymentByOrderId(orderId)
                .orElseThrow(PaymentException::orderMismatchException);
    }

    @Transactional
    @Override
    public void cancelPaymentAndSaveRefund(Long orderItemId, Long orderId, CancelOrderItemRequest request) {
        Payment payment = getPaymentByOrderId(orderId);

        CancelPaymentRequest paymentRequest = new CancelPaymentRequest(request.cancelReason(), request.cancelAmount());

        String tossResponse = tossApiClient.sendCancelRequest(payment.getTossPaymentKey(), paymentRequest);

        TossPaymentResponse response;
        try {
            response = objectMapper.readValue(tossResponse, TossPaymentResponse.class);

        } catch (JsonProcessingException e) {
            throw PaymentException.jsonParsingException();
        }

        for (CancelDto cancel : response.cancels()) {
            String transactionKey = cancel.transactionKey();

            if (refundRepository.findByTransactionKey(transactionKey).isPresent()) {
                continue;
            }

            if (cancel.cancelStatus().equals("DONE")) {
                payment.updatePaymentInfo(response, tossResponse);

                Refund refund = Refund.builder()
                        .paymentId(payment.getId())
                        .orderItemId(orderItemId)
                        .paymentKey(response.paymentKey())
                        .transactionKey(cancel.transactionKey())
                        .cancelReason(cancel.cancelReason())
                        .cancelAmount(cancel.cancelAmount())
                        .cancelStatus(cancel.cancelStatus())
                        .canceledAt(cancel.canceledAt())
                        .build();

                refundRepository.save(refund);
            }
        }
    }

    // 결제완료 알림전송
    private void sendPaymentNotification(Long memberId, String orderNum, Long totalAmount) {
        // 알림 전송
        NotificationPayload notificationPayload = NotificationPayload.of(
                memberId,
                memberId,
                NotificationType.PAYMENT,
                PAYMENT_CONFIRM,
                orderNum,
                totalAmount.toString()
        );
        notificationService.sendNotification(notificationPayload);
    }
}

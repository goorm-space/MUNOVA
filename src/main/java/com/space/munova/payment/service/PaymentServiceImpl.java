package com.space.munova.payment.service;

import com.space.munova.auth.service.AuthService;
import com.space.munova.common.validation.AmountVerifier;
import com.space.munova.coupon.service.CouponService;
import com.space.munova.notification.dto.NotificationPayload;
import com.space.munova.notification.dto.NotificationType;
import com.space.munova.notification.service.NotificationService;
import com.space.munova.order.dto.CancelOrderItemRequest;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.Order;
import com.space.munova.order.service.OrderQueryServiceImpl;
import com.space.munova.payment.client.TossApiClient;
import com.space.munova.payment.dto.CancelDto;
import com.space.munova.payment.dto.CancelPaymentRequest;
import com.space.munova.payment.dto.ConfirmPaymentRequest;
import com.space.munova.payment.dto.TossPaymentResponse;
import com.space.munova.payment.entity.Payment;
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

import static com.space.munova.payment.dto.PaymentNotification.PAYMENT_CONFIRM;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final OrderQueryServiceImpl orderQueryService;
    private final AuthService authService;
    private final TossApiClient tossApiClient;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final CouponService couponService;
    private final CartService cartService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public void confirmPaymentAndSavePayment(ConfirmPaymentRequest request, Long memberId) {
        Order order = orderQueryService.getOrderByOrderNum(request.orderId());

        authService.verifyAuthorization(order.getMember().getId(), memberId);
        validateAmount(request.amount(), order.getTotalPrice());

        TossPaymentResponse response = tossApiClient.sendConfirmRequest(request);

        eventPublisher.publishEvent(new PaymentCompensationEvent(request.paymentKey(), request.orderId(), request.amount()));

        if (!response.status().isDone()) {
            throw PaymentException.paymentStatusException(
                    String.format("PaymentStatus는 'DONE' 상태여야 합니다. 현재 상태: '%s'", response.status().name())
            );
        }

        validateAmount(response.totalAmount(), order.getTotalPrice());

        order.updateStatus(OrderStatus.PAID);

        if (order.getCouponId() != null) {
            couponService.useCoupon(order.getCouponId());
        }

        Payment payment = Payment.create(order.getId(), response);
        paymentRepository.save(payment);

        cartService.deleteByOrderItemsAndMemberId(order.getOrderItems(), memberId);

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

        CancelPaymentRequest cancelRequest = CancelPaymentRequest.of(request.cancelReason(), request.cancelAmount());
        TossPaymentResponse response = tossApiClient.sendCancelRequest(payment.getTossPaymentKey(), cancelRequest);

        for (CancelDto cancel : response.cancels()) {
            String transactionKey = cancel.transactionKey();

            if (!cancel.cancelStatus().isDone()) {
                throw PaymentException.paymentStatusException(
                        String.format("CancelStatus는 'DONE' 상태여야 합니다. 현재 상태: '%s'", cancel.cancelStatus().name())
                );
            }

            if (refundRepository.findByTransactionKey(transactionKey).isPresent()) {
                continue;
            }

            payment.updatePaymentInfo(response.status(), response.lastTransactionKey());

            Refund refund = Refund.create(payment.getId(), orderItemId, response.paymentKey(), cancel);
            refundRepository.save(refund);
        }
    }

    private void validateAmount(Long expectedAmount, Long actualAmount) {
        try {
            AmountVerifier.verify(expectedAmount, actualAmount);
        } catch (IllegalArgumentException e) {
            throw PaymentException.amountMismatchException(e.getMessage());
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

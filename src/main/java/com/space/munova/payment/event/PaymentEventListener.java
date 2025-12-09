package com.space.munova.payment.event;

import com.space.munova.coupon.service.CouponService;
import com.space.munova.notification.dto.NotificationPayload;
import com.space.munova.notification.dto.NotificationType;
import com.space.munova.notification.service.NotificationService;
import com.space.munova.order.dto.OrderStatus;
import com.space.munova.order.entity.Order;
import com.space.munova.order.service.OrderService;
import com.space.munova.payment.client.TossApiClient;
import com.space.munova.payment.dto.CancelDto;
import com.space.munova.payment.dto.CancelPaymentRequest;
import com.space.munova.payment.dto.CancelReason;
import com.space.munova.payment.dto.TossPaymentResponse;
import com.space.munova.payment.entity.Payment;
import com.space.munova.payment.entity.Refund;
import com.space.munova.payment.repository.PaymentRepository;
import com.space.munova.payment.repository.RefundRepository;
import com.space.munova.product.application.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.space.munova.payment.dto.PaymentNotification.PAYMENT_CONFIRM;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final TossApiClient tossApiClient;
    private final RefundRepository refundRepository;
    private final OrderService orderService;
    private final PaymentRepository paymentRepository;
    private final CouponService couponService;
    private final CartService cartService;
    private final NotificationService notificationService;

    /**
     * 결제 성공 시 payment save
     */
    @EventListener
    @Async//("paymentExecutor")
    @Transactional
    public void savePayment(PaymentSuccessEvent event) {
        Order order = orderService.getOrderByOrderNum(event.orderNum());
        Payment payment = Payment.create(order.getId(), event);
        paymentRepository.save(payment);

    }

    /**
     * 결제 성공 시 order update
     */
    @EventListener
    @Async//("paymentExecutor")
    @Transactional
    public void updateOrder(PaymentSuccessEvent event) {
        Order order = orderService.getOrderByOrderNum(event.orderNum());
        order.updateStatus(OrderStatus.PAID);

    }

    /**
     * 결제 성공 시 coupon 사용
     */
    @EventListener
    @Async//("paymentExecutor")
    @Transactional
    public void useCoupon(PaymentSuccessEvent event) {
        Order order = orderService.getOrderByOrderNum(event.orderNum());
        if (order.getCouponId() != null) {
            couponService.useCoupon(order.getCouponId());
        }

    }

    /**
     * 결제 성공 시 carcart 삭제 비동기
     */
    @EventListener
    @Async//("paymentExecutor")
    public void deleteCart(PaymentSuccessEvent event) {
        Order order = orderService.getOrderWithItems(event.orderNum());
        cartService.deleteByOrderItemsAndMemberId(order.getOrderItems(), order.getMemberId());
    }

    /**
     * 알림 발송 비동기
     */
    @EventListener
    @Async//("paymentExecutor")
    public void sendPaymentNotification(PaymentSuccessEvent event) {
        // 알림 전송
        NotificationPayload notificationPayload = NotificationPayload.of(
                event.memberId(),
                event.memberId(),
                NotificationType.PAYMENT,
                PAYMENT_CONFIRM,
                event.orderNum(),
                event.totalAmount().toString()
        );
        notificationService.sendNotification(notificationPayload);
    }


    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onPaymentRollback(PaymentFailureEvent event) {
        String paymentKey = event.paymentKey();

        if (refundRepository.existsByPaymentKey(paymentKey)) {
            return;
        }

        TossPaymentResponse response = tossApiClient.sendCancelRequest(paymentKey,
                CancelPaymentRequest.of(CancelReason.ROLLBACK_COMPENSATION, event.amount()));

        for (CancelDto cancel : response.cancels()) {

            if (cancel.cancelStatus().isDone()) {
                refundRepository.save(Refund.createWhenRollBack(paymentKey, cancel));
            }
        }
    }
}

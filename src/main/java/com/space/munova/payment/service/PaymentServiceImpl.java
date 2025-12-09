package com.space.munova.payment.service;

import com.space.munova.auth.service.AuthService;
import com.space.munova.common.validation.AmountVerifier;
import com.space.munova.notification.dto.NotificationPayload;
import com.space.munova.notification.dto.NotificationType;
import com.space.munova.notification.service.NotificationService;
import com.space.munova.order.dto.CancelOrderItemRequest;
import com.space.munova.order.dto.redis.TmpOrderDto;
import com.space.munova.order.service.RedisService;
import com.space.munova.payment.client.TossApiClient;
import com.space.munova.payment.dto.CancelDto;
import com.space.munova.payment.dto.CancelPaymentRequest;
import com.space.munova.payment.dto.ConfirmPaymentRequest;
import com.space.munova.payment.dto.TossPaymentResponse;
import com.space.munova.payment.entity.Payment;
import com.space.munova.payment.entity.Refund;
import com.space.munova.payment.event.PaymentSuccessEvent;
import com.space.munova.payment.exception.PaymentException;
import com.space.munova.payment.repository.PaymentRepository;
import com.space.munova.payment.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.space.munova.payment.dto.PaymentNotification.PAYMENT_CONFIRM;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final AuthService authService;
    private final TossApiClient tossApiClient;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisService redisService;

    @Transactional
    @Override
    public void confirmPaymentAndSavePayment(ConfirmPaymentRequest request, Long memberId) {
        // 주문 만료 여부 검증
        TmpOrderDto tmpOrderDto = redisService.getTemporaryOrder(request.orderId());

        // 주문자 및 금액 검증
        authService.verifyAuthorization(tmpOrderDto.memberId(), memberId);
        validateAmount(request.amount(), tmpOrderDto.finalPrice());

        // 재고 검증 및 차감
        redisService.validateAndDecreaseStock(tmpOrderDto.items());

        // --- 아래는 비동기로직으로 돌려도 된다
        // Todo: 토스 api 비동기 ??
//        TossPaymentResponse response = tossApiClient.sendConfirmRequest(request);
        tossApiClient.asyncConfirmRequest(request)
                .thenAccept(response -> {
                    PaymentSuccessEvent successEvent = PaymentSuccessEvent.from(memberId, response);
                    redisService.deleteTmpOrder(response.orderId());
                    eventPublisher.publishEvent(successEvent);
                })
                .exceptionally(ex -> {
                    // Todo: 결제 재시도 로직

                    log.error("통신 실패, 재고 복구");
                    redisService.restoreStock(tmpOrderDto.items());
                    return null;
                });
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

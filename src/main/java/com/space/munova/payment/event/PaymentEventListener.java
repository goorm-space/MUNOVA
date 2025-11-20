package com.space.munova.payment.event;

import com.space.munova.payment.client.TossApiClient;
import com.space.munova.payment.dto.*;
import com.space.munova.payment.entity.Refund;
import com.space.munova.payment.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final TossApiClient tossApiClient;
    private final RefundRepository refundRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onPaymentRollback(PaymentCompensationEvent event) {
        String paymentKey = event.paymentKey();

        if (refundRepository.existsByPaymentKey(paymentKey)) {
            return;
        }

        TossPaymentResponse response = tossApiClient.sendCancelRequest(paymentKey,
                CancelPaymentRequest.of(CancelReason.ROLLBACK_COMPENSATION, event.amount()));

        for(CancelDto cancel : response.cancels()) {

            if (cancel.cancelStatus().isDone()) {
                refundRepository.save(Refund.createWhenRollBack(paymentKey, cancel));
            }
        }
    }
}

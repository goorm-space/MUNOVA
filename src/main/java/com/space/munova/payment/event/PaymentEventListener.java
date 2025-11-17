package com.space.munova.payment.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.payment.client.TossApiClient;
import com.space.munova.payment.dto.*;
import com.space.munova.payment.entity.Refund;
import com.space.munova.payment.exception.PaymentException;
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
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onPaymentRollback(ConfirmPaymentRequest event) {
        String paymentKey = event.paymentKey();

        if (refundRepository.existsByPaymentKey(paymentKey)) {
            return;
        }

            String cancelResponse = tossApiClient.sendCancelRequest(paymentKey,
                    new CancelPaymentRequest(CancelReason.ROLLBACK_COMPENSATION, event.amount()));

        TossPaymentResponse response;
        try {
            response = objectMapper.readValue(cancelResponse, TossPaymentResponse.class);
        } catch (JsonProcessingException e) {
            throw PaymentException.jsonParsingException();
        }

        for(CancelDto cancel : response.cancels()) {
            String transactionKey = cancel.transactionKey();

            if (cancel.cancelStatus().equals("DONE")) {
                refundRepository.save(Refund.builder()
                                .paymentKey(paymentKey)
                                .transactionKey(transactionKey)
                                .cancelReason(cancel.cancelReason())
                                .cancelAmount(cancel.cancelAmount())
                                .canceledAt(cancel.canceledAt())
                                .build());
            }
        }


    }
}

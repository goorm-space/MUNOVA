package com.space.munova.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CancelDto(
        String transactionKey,
        String cancelReason,
        ZonedDateTime canceledAt,
        Integer transferDiscountAmount,
        Integer easyPayDiscountAmount,
        String receiptKey,
        Integer cancelAmount,
        Integer taxFreeAmount,
        Integer refundableAmount,
        String cancelStatus,
        String cancelRequestId
) {
}

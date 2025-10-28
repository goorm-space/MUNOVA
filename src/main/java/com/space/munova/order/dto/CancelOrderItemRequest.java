package com.space.munova.order.dto;

import com.space.munova.payment.dto.CancelReason;

public record CancelOrderItemRequest(
        CancelType cancelType,
        CancelReason cancelReason,
        Long cancelAmount
) {
}

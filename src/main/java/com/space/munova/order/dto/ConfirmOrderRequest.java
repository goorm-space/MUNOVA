package com.space.munova.order.dto;

public record ConfirmOrderRequest(
        Long orderId,
        String orderNum,
        String userRequest,
        Long orderCouponId,
        Long clientCalculatedAmount
) {
}

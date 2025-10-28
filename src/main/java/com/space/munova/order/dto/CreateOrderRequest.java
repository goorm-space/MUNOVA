package com.space.munova.order.dto;

import java.util.List;

public record CreateOrderRequest(
        Long orderCouponId,
        String userRequest,
        Long clientCalculatedAmount,
        List<OrderItemRequest> orderItems
) {
}

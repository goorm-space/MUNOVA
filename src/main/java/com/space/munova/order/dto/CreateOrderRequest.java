package com.space.munova.order.dto;

import java.util.List;

public record CreateOrderRequest(
        OrderType type,
        List<Long> cartIds,
        List<OrderItemRequest> orderItems
) {
}

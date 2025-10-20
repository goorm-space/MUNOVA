package com.space.munova.order.dto;

public record OrderItemRequest(
        Long productId,
        Integer quantity,
        Long couponId
) {
}

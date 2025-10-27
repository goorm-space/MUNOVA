package com.space.munova.order.dto;

public record OrderItemRequest(
        Long productDetailId,
        Integer quantity
) {
}

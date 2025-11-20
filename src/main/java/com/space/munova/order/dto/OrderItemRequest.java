package com.space.munova.order.dto;

import com.space.munova.order.exception.OrderItemException;

import java.util.List;

public record OrderItemRequest(
        Long productDetailId,
        Integer quantity
) {
    public OrderItemRequest {
        if (productDetailId == null || productDetailId <= 0) {
            throw OrderItemException.invalidItemId();
        }

        if (quantity == null || quantity <= 0) {
            throw OrderItemException.invalidQuantity("현재 수량: " + quantity);
        }
    }
}

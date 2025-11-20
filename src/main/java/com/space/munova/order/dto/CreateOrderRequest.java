package com.space.munova.order.dto;

import com.space.munova.order.exception.OrderException;
import com.space.munova.order.exception.OrderItemException;

import java.util.List;

public record CreateOrderRequest(
        Long orderCouponId,
        String userRequest,
        Long clientCalculatedAmount,
        List<OrderItemRequest> orderItems
) {
    public CreateOrderRequest {
        if (clientCalculatedAmount == null || clientCalculatedAmount < 0) {
            throw OrderException.invalidClientCalculatedAmount();
        }

        if (orderItems == null || orderItems.isEmpty()) {
            throw OrderItemException.noOrderItemsNotAllowedException();
        }
    }
}

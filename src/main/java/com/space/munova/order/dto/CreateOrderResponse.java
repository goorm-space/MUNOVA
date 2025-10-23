package com.space.munova.order.dto;

import com.space.munova.order.entity.Order;

public record CreateOrderResponse(
    Long orderId,
    String orderNum,
    Long totalPrice
) {
    public static CreateOrderResponse from(Order order) {
        return new CreateOrderResponse(
                order.getId(),
                order.getOrderNum(),
                order.getTotalPrice()
        );
    }
}

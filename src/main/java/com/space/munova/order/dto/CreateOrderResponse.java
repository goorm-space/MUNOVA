package com.space.munova.order.dto;

import com.space.munova.order.entity.Order;

import java.util.List;

public record CreateOrderResponse(
    Long orderId,
    String orderNum,
    String userName,
    String address,
    List<OrderItemDto> orderItems
) {
    public static CreateOrderResponse from(Order order) {
        List<OrderItemDto> orderItems = order.getOrderItems().stream()
                .map(OrderItemDto::from)
                .toList();

        return new CreateOrderResponse(
                order.getId(),
                order.getOrderNum(),
                order.getMember().getUsername(),
                order.getMember().getAddress(),
                orderItems
        );
    }
}

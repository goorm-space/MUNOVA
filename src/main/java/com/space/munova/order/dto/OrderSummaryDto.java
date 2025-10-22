package com.space.munova.order.dto;

import com.space.munova.order.entity.Order;

import java.time.LocalDateTime;
import java.util.List;

public record OrderSummaryDto(
        Long orderId,
        LocalDateTime orderDate,
        List<OrderItemDetailResponse> orderItems
) {
    public static OrderSummaryDto from(Order order) {
        List<OrderItemDetailResponse> orderItems = order.getOrderItems().stream()
                .map(OrderItemDetailResponse::from)
                .toList();

        return new OrderSummaryDto(
                order.getId(),
                order.getCreatedAt(),
                orderItems
        );
    }
}

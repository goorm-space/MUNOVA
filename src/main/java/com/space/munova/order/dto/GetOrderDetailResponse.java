package com.space.munova.order.dto;

import com.space.munova.order.entity.Order;

import java.time.LocalDateTime;
import java.util.List;

public record GetOrderDetailResponse (
        Long orderId,
        String orderNum,
        String username,
        String address,
        String userRequest,
        OrderStatus status,
        Long originPrice,
        int discountPrice,
        Long totalPrice,
        LocalDateTime orderDate,
        String paymentReceipt,
        String paymentMethod,
        List<OrderItemDto> orderItems

) {
    public static GetOrderDetailResponse from(Order order) {
        List<OrderItemDto> orderItems = order.getOrderItems().stream()
                .map(OrderItemDto::from)
                .toList();

        return new GetOrderDetailResponse(
                order.getId(),
                order.getOrderNum(),
                order.getMember().getUsername(),
                order.getMember().getAddress(),
                order.getUserRequest(),
                order.getStatus(),
                order.getOriginPrice(),
                order.getDiscountPrice(),
                order.getTotalPrice(),
                order.getCreatedAt(),
                "www.paymentReceipt",
                "KAKAOPAY",
                orderItems
        );
    }
}

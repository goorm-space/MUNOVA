package com.space.munova.order.dto;

import com.space.munova.order.entity.Order;
import com.space.munova.payment.entity.Payment;
import com.space.munova.payment.entity.PaymentMethod;

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
        Long discountPrice,
        Long totalPrice,
        LocalDateTime orderDate,
        String paymentReceipt,
        PaymentMethod paymentMethod,
        List<OrderItemDto> orderItems

) {
    public static GetOrderDetailResponse from(Order order, Payment payment) {
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
                payment.getReceipt(),
                payment.getMethod(),
                orderItems
        );
    }
}

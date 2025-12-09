package com.space.munova.order.dto;

import com.space.munova.order.entity.Order;

public record PaymentPrepareResponse(
        String userName,
        String orderId,
        Long amount,
        String firstProductName,
        int orderItemQuantity
) {
    public static PaymentPrepareResponse from(String userName, Order order) {
        return new PaymentPrepareResponse(
                userName,
                order.getOrderNum(),
                order.getTotalPrice(),
                order.getOrderItems().getFirst().getNameSnapshot(),
                order.getOrderItems().size()
        );
    }
}

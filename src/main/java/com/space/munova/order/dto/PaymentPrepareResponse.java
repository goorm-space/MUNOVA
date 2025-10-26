package com.space.munova.order.dto;

import com.space.munova.order.entity.Order;

import java.util.List;

public record PaymentPrepareResponse (
        String userName,
        String orderId,
        Long amount,
        String firstProductName,
        int orderItemQuantity
){
    public static PaymentPrepareResponse from(Order order){
        return new PaymentPrepareResponse(
                order.getMember().getUsername(),
                order.getOrderNum(),
                order.getTotalPrice(),
                order.getOrderItems().getFirst().getNameSnapshot(),
                order.getOrderItems().size()
        );
    }
}

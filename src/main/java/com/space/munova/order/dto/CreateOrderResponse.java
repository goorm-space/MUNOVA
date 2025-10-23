package com.space.munova.order.dto;

import com.space.munova.order.entity.Order;

public record CreateOrderResponse(
    Long orderId,
    String orderNum,
    String userName,
    String address,
    String userRequest,
    Long totalPrice
    // Todo: 결제 수단 가져오자
) {
    public static CreateOrderResponse from(Order order) {
        return new CreateOrderResponse(
                order.getId(),
                order.getOrderNum(),
                order.getMember().getUsername(),
                order.getMember().getAddress(),
                order.getUserRequest(),
                order.getTotalPrice()
        );
    }
}

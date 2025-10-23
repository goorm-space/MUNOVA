package com.space.munova.order.dto;

import com.space.munova.order.entity.OrderItem;
import com.space.munova.order.entity.OrderStatus;

public record OrderItemDetailResponse(
        Long orderItemId,
        String productName,
        // Todo: 판매자, 옵션정보도 불러와야한다
        int quantity,
        Long totalPrice,
        OrderStatus status
) {
    public static OrderItemDetailResponse from(OrderItem orderItem) {
        return new OrderItemDetailResponse(
                orderItem.getId(),
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getOriginPrice() * orderItem.getQuantity(),
                orderItem.getStatus()
        );
    }
}

package com.space.munova.order.dto;

import com.space.munova.order.entity.OrderItem;

public record OrderItemDto(
        Long orderItemId,
        String productName,
        // Todo: 판매자, 옵션정보, 이미지도 불러와야한다
        int quantity,
        Long totalPrice,
        OrderStatus status
) {
    public static OrderItemDto from(OrderItem orderItem) {
        return new OrderItemDto(
                orderItem.getId(),
                orderItem.getNameSnapshot(),
                orderItem.getQuantity(),
                orderItem.getPriceSnapshot() * orderItem.getQuantity(),
                orderItem.getStatus()
        );
    }
}
